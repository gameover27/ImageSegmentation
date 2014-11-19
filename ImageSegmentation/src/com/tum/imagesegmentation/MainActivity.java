/*
 * Copyright (C) 2014 Magdalena Neumann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tum.imagesegmentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageSobelEdgeDetection;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.app.ApplicationErrorReport.AnrInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v8.renderscript.*;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * This Class provides the main frame for the ImageSegmentation app
 * 
 * @author Magdalena Neumann
 * 
 */
public class MainActivity extends ActionBarActivity {
	FragmentPagerAdapter viewPagerAdapter;
	private Menu actionBar;
	private boolean draw_mode_imgview_save = false;
	private boolean fgbg_save = true;
	public boolean scaled;
	private ProgressDialog progress;
	private int scaled_width;
	private int scaled_height;
	Uri imageURI = null;
	private Uri scribbleURI = null;
	WakeLock wakeLock;

	long starttime = 0;

	Bitmap image_original;
	Bitmap image_scaled;

	ByteBuffer pathbitmapfgBuffer = null;
	ByteBuffer pathbitmapbgBuffer = null;
	ByteBuffer imageGradientBuffer = null;
	ByteBuffer imageBuffer = null;
	ByteBuffer uBuffer = null;
	
	RenderScript mRS;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (((TouchImageView) findViewById(R.id.imageView1)).getDrawable() != null
				&& ((TouchImageView) findViewById(R.id.imageView1))
						.getDrawable() instanceof BitmapDrawable
				&& ((BitmapDrawable) ((TouchImageView) findViewById(R.id.imageView1))
						.getDrawable()).getBitmap() != null
				&& !((BitmapDrawable) ((TouchImageView) findViewById(R.id.imageView1))
						.getDrawable()).getBitmap().isRecycled()) {
			savedInstanceState
					.putParcelable(
							"SegPic",
							((BitmapDrawable) ((TouchImageView) findViewById(R.id.imageView1))
									.getDrawable()).getBitmap());
			try {
				savedInstanceState.putSerializable("foreground",
						(((TouchImageView) findViewById(R.id.imageView1))
								.getForegroundPaths()));
				savedInstanceState.putSerializable("background",
						(((TouchImageView) findViewById(R.id.imageView1))
								.getBackgroundPaths()));
			} catch (Exception e) {
				return;
			}
		}
		if (((ImageView) findViewById(R.id.imageView2)).getVisibility() == View.VISIBLE
				&& ((ImageView) findViewById(R.id.imageView2)).getDrawable() != null
				&& ((ImageView) findViewById(R.id.imageView2)).getDrawable() instanceof BitmapDrawable
				&& ((BitmapDrawable) ((ImageView) findViewById(R.id.imageView2))
						.getDrawable()).getBitmap() != null
				&& !((BitmapDrawable) ((ImageView) findViewById(R.id.imageView2))
						.getDrawable()).getBitmap().isRecycled()) {
			savedInstanceState
					.putParcelable(
							"UPic",
							((BitmapDrawable) ((ImageView) findViewById(R.id.imageView2))
									.getDrawable()).getBitmap());
		}

		if (actionBar != null) {
			savedInstanceState.putBoolean("draw_mode_imgview",
					draw_mode_imgview_save);
			if (((TouchImageView) findViewById(R.id.imageView1))
					.getClustertype() == TouchImageView.Clustertype.FOREGROUND) {
				savedInstanceState.putBoolean("fgbg", true);
			} else {
				savedInstanceState.putBoolean("fgbg", false);
			}
		}
		if (imageURI != null) {
			savedInstanceState.putParcelable("uri", imageURI);
		}

		if (scribbleURI != null) {
			savedInstanceState.putParcelable("scribbleuri", scribbleURI);
		}

		super.onSaveInstanceState(savedInstanceState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.getParcelable("uri") != null) {
			imageURI = savedInstanceState.getParcelable("uri");
			reloadImage(imageURI, false);
		} else {
			imageURI = null;
		}
		if (savedInstanceState.getParcelable("scribbleuri") != null) {
			scribbleURI = savedInstanceState.getParcelable("scribbleuri");
		} else {
			scribbleURI = null;
		}
		if (savedInstanceState.getParcelable("SegPic") != null) {
			findViewById(R.id.imageView1).setVisibility(View.VISIBLE);
			((TouchImageView) findViewById(R.id.imageView1))
					.setImageBitmap(savedInstanceState.getParcelable("SegPic") instanceof Bitmap ? (Bitmap) savedInstanceState
							.getParcelable("SegPic") : null);
		} else {
			findViewById(R.id.imageView1).setVisibility(View.GONE);
		}
		if (savedInstanceState.getParcelable("UPic") != null) {
			findViewById(R.id.imageView2).setVisibility(View.VISIBLE);
			((ImageView) findViewById(R.id.imageView2))
					.setImageBitmap(savedInstanceState.getParcelable("UPic") instanceof Bitmap ? (Bitmap) savedInstanceState
							.getParcelable("UPic") : null);
		} else {
			findViewById(R.id.imageView2).setVisibility(View.GONE);
		}
		if (savedInstanceState.getSerializable("foreground") != null) {
			((TouchImageView) findViewById(R.id.imageView1))
					.setForegroundPaths(savedInstanceState
							.getSerializable("foreground") instanceof ColoredPaths ? (ColoredPaths) savedInstanceState
							.getSerializable("foreground") : null);
		}
		if (savedInstanceState.getSerializable("background") != null) {
			((TouchImageView) findViewById(R.id.imageView1))
					.setBackgroundPaths(savedInstanceState
							.getSerializable("background") instanceof ColoredPaths ? (ColoredPaths) savedInstanceState
							.getSerializable("background") : null);
		}

		draw_mode_imgview_save = savedInstanceState
				.getBoolean("draw_mode_imgview");
		fgbg_save = savedInstanceState.getBoolean("fgbg");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		ViewPager vPager = (ViewPager) findViewById(R.id.vPager);

		viewPagerAdapter = new MyPagerAdapter(getFragmentManager());
		vPager.setAdapter(viewPagerAdapter);

		vPager.setCurrentItem(1);
		// keep two offscreen pages in memory
		vPager.setOffscreenPageLimit(2);
		
        /*
         * Renderscript init		
         */
        mRS = RenderScript.create(this);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		actionBar = menu;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (actionBar != null) {
			if (draw_mode_imgview_save) {
				actionBar.findItem(R.id.action_open_pic).setVisible(false);
				actionBar.findItem(R.id.action_draw_mode).setIcon(
						getTheme().obtainStyledAttributes(
								new int[] { R.attr.menuDrawModeIconRed })
								.getResourceId(0, 0));
				actionBar.setGroupVisible(R.id.draw_options, true);
				((TouchImageView) findViewById(R.id.imageView1))
						.setDrawMode(scribbleURI == null ? true : false);
			} else {
				actionBar.findItem(R.id.action_open_pic).setVisible(true);
				actionBar.findItem(R.id.action_draw_mode).setIcon(
						getTheme().obtainStyledAttributes(
								new int[] { R.attr.menuDrawModeIcon })
								.getResourceId(0, 0));
				actionBar.setGroupVisible(R.id.draw_options, false);
				((TouchImageView) findViewById(R.id.imageView1))
						.setDrawMode(false);
			}

			if (fgbg_save) {
				((TouchImageView) findViewById(R.id.imageView1))
						.setClustertype(TouchImageView.Clustertype.FOREGROUND);
				actionBar.findItem(R.id.action_switch_fore_back).setIcon(
						getTheme().obtainStyledAttributes(
								new int[] { R.attr.menuForegroundIcon })
								.getResourceId(0, 0));
			} else {
				((TouchImageView) findViewById(R.id.imageView1))
						.setClustertype(TouchImageView.Clustertype.BACKGROUND);
				actionBar.findItem(R.id.action_switch_fore_back).setIcon(
						getTheme().obtainStyledAttributes(
								new int[] { R.attr.menuBackgroundIcon })
								.getResourceId(0, 0));
			}
			if (((TouchImageView) findViewById(R.id.imageView1))
					.getVisibility() != View.VISIBLE) {
				actionBar.findItem(R.id.action_start_segmentation).setVisible(
						false);
			}
			if (((ImageView) findViewById(R.id.imageView2)).getVisibility() != View.VISIBLE) {
				actionBar.findItem(R.id.action_save_pic).setVisible(false);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_open_pic) {
			openGallery(Constants.GALLERY_IMAGE_ID);
			return true;
		}
		if (id == R.id.action_draw_mode) {
			if (draw_mode_imgview_save) {
				draw_mode_imgview_save = false;
				((TouchImageView) findViewById(R.id.imageView1))
						.setDrawMode(false);
				item.setIcon(getTheme().obtainStyledAttributes(
						new int[] { R.attr.menuDrawModeIcon }).getResourceId(0,
						0));
				actionBar.setGroupVisible(R.id.draw_options, false);
				actionBar.findItem(R.id.action_open_pic).setVisible(true);
				Toast.makeText(getApplicationContext(), "Draw Mode Disabled",
						Toast.LENGTH_SHORT).show();
			} else {
				draw_mode_imgview_save = true;
				if (scribbleURI == null) {
					((TouchImageView) findViewById(R.id.imageView1))
							.setDrawMode(true);
				}
				item.setIcon(getTheme().obtainStyledAttributes(
						new int[] { R.attr.menuDrawModeIconRed })
						.getResourceId(0, 0));
				actionBar.setGroupVisible(R.id.draw_options, true);
				actionBar.findItem(R.id.action_open_pic).setVisible(false);
				Toast.makeText(getApplicationContext(), "Draw Mode Enabled",
						Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		if (id == R.id.action_clear_draw) {
			((TouchImageView) findViewById(R.id.imageView1))
					.clearDrawingPaths();
			scribbleURI = null;

			if (draw_mode_imgview_save) {
				((TouchImageView) findViewById(R.id.imageView1))
						.setDrawMode(true);
			} else {
				((TouchImageView) findViewById(R.id.imageView1))
						.setDrawMode(false);
			}
			return true;
		}
		if (id == R.id.action_start_segmentation) {

			reloadImage(imageURI, false);

			Bitmap pathbitmapfg;
			Bitmap pathbitmapbg;
			if (scribbleURI == null) {
				pathbitmapfg = ((TouchImageView) findViewById(R.id.imageView1))
						.getPathBitmap(true, scaled_width, scaled_height);
				pathbitmapbg = ((TouchImageView) findViewById(R.id.imageView1))
						.getPathBitmap(false, scaled_width, scaled_height);
			} else {
				pathbitmapfg = getScribbleFGBG(true);
				pathbitmapbg = getScribbleFGBG(false);
			}

			if (pathbitmapfg != null && pathbitmapbg != null) {
				scheduleProcessing(image_scaled, pathbitmapfg, pathbitmapbg);
			}
		}

		if (id == R.id.action_switch_fore_back) {
			if (((TouchImageView) findViewById(R.id.imageView1))
					.getClustertype() == TouchImageView.Clustertype.FOREGROUND) {
				((TouchImageView) findViewById(R.id.imageView1))
						.setClustertype(TouchImageView.Clustertype.BACKGROUND);
				item.setIcon(getTheme().obtainStyledAttributes(
						new int[] { R.attr.menuBackgroundIcon }).getResourceId(
						0, 0));
				Toast.makeText(getApplicationContext(), "Background",
						Toast.LENGTH_SHORT).show();
			} else if (((TouchImageView) findViewById(R.id.imageView1))
					.getClustertype() == TouchImageView.Clustertype.BACKGROUND) {
				((TouchImageView) findViewById(R.id.imageView1))
						.setClustertype(TouchImageView.Clustertype.FOREGROUND);
				item.setIcon(getTheme().obtainStyledAttributes(
						new int[] { R.attr.menuForegroundIcon }).getResourceId(
						0, 0));
				Toast.makeText(getApplicationContext(), "Foreground",
						Toast.LENGTH_SHORT).show();
			}
		}

		if (id == R.id.action_save_pic) {
			saveImage();
		}

		if (id == R.id.action_load_scribbles) {
			openGallery(Constants.GALLERY_SCRIBBLE_ID);
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * This method initializes the progress dialog which is updated during
	 * computation.
	 */
	private void setUpProgress() {
		// Create progress dialog
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		progress = new ProgressDialog(this);
		progress.setMax(sp.getInt("pref_iterations", 350) + 1);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setTitle("Computing");
		progress.setMessage("Please wait");
		progress.setIndeterminate(false);
		progress.setCancelable(false);
		progress.setProgress(0);
		progress.show();
	}

	/**
	 * This method can be called from outside the main class to increment the
	 * progessdialog by 1.
	 */
	public void incrementProgress() {
		progress.incrementProgressBy(1);
	}

	/**
	 * This method handles the process of closing the progress dialog.
	 */
	private void dismissProgress() {
		if (progress.isShowing())
			progress.dismiss();
	}

	/**
	 * This method acts as a callback function to process the data calculated by
	 * the segmentation algorithm implemented in C via the NDK
	 * 
	 * @param buf
	 *            A processed buffer containing the u-matrix
	 */
	private void callbackSegmentation(ByteBuffer buf) {
		Bitmap u = Bitmap.createBitmap(scaled_width, scaled_height,
				Bitmap.Config.ALPHA_8);

		imageGradientBuffer = null;

		Bitmap scaledbmp = Bitmap.createScaledBitmap(u,
				image_original.getWidth(), image_original.getHeight(), true);
		u.recycle();
		u = scaledbmp;
		scaledbmp = null;

		System.gc();

		imageBuffer.flip();
		uBuffer.flip();

		// Convert back to argb
		Bitmap rgb_u = Bitmap.createBitmap(u.getWidth(), u.getHeight(),
				Bitmap.Config.ARGB_8888);
		rgb_u.eraseColor(Color.BLACK);
		float[] matrix = new float[] { 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0,
				1, 0, 0, 0, 0, 1, 0 };
		Paint convRGB = new Paint();
		convRGB.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(
				matrix)));
		convRGB.setAlpha(255);
		Canvas rgbCanv = new Canvas(rgb_u);
		rgbCanv.setDensity(Bitmap.DENSITY_NONE);
		rgbCanv.drawBitmap(u, 0, 0, convRGB);

		u.recycle();
		u = rgb_u;
		rgb_u = null;

		displayU(u);

		// Draw contours to the original image
		new DrawContoursThread(
				imageBuffer,
				uBuffer,
				image_original.getHeight(),
				image_original.getWidth(),
				Constants.CONTOUR_THRESHOLD,
				(image_original.getWidth() > (1.0 / Constants.CONTOUR_WIDTH_RATIO)) ? (int) (Constants.CONTOUR_WIDTH_RATIO * image_original
						.getWidth()) : 1).execute();
	}

	/**
	 * This method acts as a callback function to process the data calculated by
	 * the contour drawing algorithm implemented in C via the NDK
	 * 
	 * @param buf
	 *            A processed buffer containing the original image with drawn
	 *            contours
	 */
	private void callbackDrawContours(ByteBuffer buf) {
		displayImage(image_original, false);

		long duration = System.currentTimeMillis() - starttime;

		actionBar.findItem(R.id.action_save_pic).setVisible(true);
		wakeLock.release();

		Toast.makeText(getApplicationContext(),
				"Calculation took " + (duration / 1000) + " s",
				Toast.LENGTH_LONG).show();

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Calculation finished");
		alertDialog.setMessage("Calculation took " + (duration / 1000) + " s");

		new AlertDialog.Builder(this)
				.setTitle("Calculation finished")
				.setMessage("Calculation took " + (duration / 1000) + " s")
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						}).show();

		imageBuffer = null;
		uBuffer = null;

		System.gc();
	}

	/**
	 * This method prepares the buffers needed for segmentation and contour
	 * drawing via NDK
	 * 
	 * @param bmp
	 *            Image to be processed
	 * @param pathbitmapfg
	 *            Image containing the foreground scribbles
	 * @param pathbitmapbg
	 *            Image containing the background scribbles
	 */
	public void scheduleProcessing(Bitmap bmp, Bitmap pathbitmapfg,
			Bitmap pathbitmapbg) {
		PowerManager mgr = (PowerManager) getApplicationContext()
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
		wakeLock.acquire();

		starttime = System.currentTimeMillis();
		if (bmp.getHeight() == pathbitmapfg.getHeight()
				&& bmp.getWidth() == pathbitmapbg.getWidth()) {
			
			// Alloc buffer for image gradient on native side

			// Run Sobel
			GPUImage gpuI = new GPUImage(this);
			gpuI.setImage(bmp); // this loads image on the current thread,
								// should be run in a thread
			gpuI.setFilter(new GPUImageSobelEdgeDetection());
			

			Bitmap imageGradient = gpuI.getBitmapWithFilterApplied();

			// convert to alpha
			/*Bitmap alpha_bmp = Bitmap.createBitmap(bmp.getWidth(),
					bmp.getHeight(), Bitmap.Config.ALPHA_8);
			float[] matrix = new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
					0, 0, 0, 1, 0, 0, 0, 0 };
			Paint convAlpha = new Paint();
			convAlpha.setColorFilter(new ColorMatrixColorFilter(
					new ColorMatrix(matrix)));
			Canvas alphaCanv = new Canvas(alpha_bmp);
			alphaCanv.setDensity(Bitmap.DENSITY_NONE);
			alphaCanv.drawBitmap(bmp, 0, 0, convAlpha);

			alpha_bmp.recycle();*/

			// Alloc buffer for user input on native side

			// Process buffers
			setUpProgress();
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext()); 
			int iterations = sp.getInt("pref_iterations", 350);
			/*
			new SegmentationThread(imageGradientBuffer, pathbitmapfgBuffer,
					pathbitmapbgBuffer, bmp.getHeight(), bmp.getWidth(),
					sp.getInt("pref_iterations", 350), Constants.ALPHA,
					Constants.BETA, Constants.TAU, Constants.THETA).execute(); */
			
	        
	        //displayU(pathbitmapfg);
	        
	        /*
	         * Read Scribbles
	         */
	        Allocation scribbleAlloc = Allocation.createFromBitmap(mRS, pathbitmapfg, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        Allocation imgGradAlloc = Allocation.createFromBitmap(mRS, imageGradient, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        
	        //Create u array
			float[] u = new float[scribbleAlloc.getType().getX() * scribbleAlloc.getType().getY()];
			float[] p_x = new float[scribbleAlloc.getType().getX() * scribbleAlloc.getType().getY()];
			float[] p_y = new float[scribbleAlloc.getType().getX() * scribbleAlloc.getType().getY()];
			
			Type uType = new Type.Builder(mRS, Element.F32(mRS)).setX(scribbleAlloc.getType().getX()).setY(scribbleAlloc.getType().getY()).create();
	        Allocation uAllocation = Allocation.createTyped(mRS, uType);
	        Allocation p_x_alloc = Allocation.createTyped(mRS, uType);
	        Allocation p_y_alloc = Allocation.createTyped(mRS, uType);
	        uAllocation.copy2DRangeFrom(0, 0, scribbleAlloc.getType().getX(), scribbleAlloc.getType().getY(), u);
	        uAllocation.copy2DRangeFrom(0, 0, scribbleAlloc.getType().getX(), scribbleAlloc.getType().getY(), p_x);
	        uAllocation.copy2DRangeFrom(0, 0, scribbleAlloc.getType().getX(), scribbleAlloc.getType().getY(), p_y);
	        
	        //Instantiate readscribble script
	        ScriptC_projectionToConstraint projectionToConstraint = new ScriptC_projectionToConstraint(mRS);
	        
	        //Assingn input and output allocations
	        projectionToConstraint.set_gIn(scribbleAlloc);
	        projectionToConstraint.set_gOut(uAllocation);
	        //readscribbles.bind_gPixels(scribbleAlloc);
	        projectionToConstraint.set_gScript(projectionToConstraint);
	        projectionToConstraint.set_foreground(1);
	        projectionToConstraint.set_initialized(0);
	        
	        //Run script for foreground
	        projectionToConstraint.invoke_filter();
	        scribbleAlloc.destroy();
	        	        
	        //Assign input for background
	        scribbleAlloc = Allocation.createFromBitmap(mRS, pathbitmapbg, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        projectionToConstraint.set_gIn(scribbleAlloc);
	        projectionToConstraint.set_foreground(0);
	        projectionToConstraint.set_initialized(1);
	        projectionToConstraint.invoke_filter();
	        scribbleAlloc.destroy();
	        
	        /*
	         * Perform segmentation
	         */
	        //Instantiate segmentationU script	        
	        ScriptC_segmentationU segU = new ScriptC_segmentationU(mRS);
	        
	     	//Instantiate segmentationP script
	        ScriptC_segmentationP segP = new ScriptC_segmentationP(mRS);
	        
	        //Bind allocations U
	        segU.set_u(uAllocation);
	        segU.set_p_x(p_x_alloc);
	        segU.set_p_y(p_y_alloc);
	        segU.set_theta(Constants.THETA);
	        segU.set_gScript(segU);
	        
	        //Bind allocations P
	        segP.set_alpha(Constants.ALPHA);
	        segP.set_beta(Constants.BETA);
	        segP.set_imgGrad(imgGradAlloc);
	        segP.set_p_x(p_x_alloc);
	        segP.set_p_y(p_y_alloc);
	        segP.set_tau(Constants.TAU);
	        segP.set_theta(Constants.THETA);
	        segP.set_u(uAllocation);
	        segP.set_gScript(segP);
	        
	        /*segP.set_u(uAllocation);
	        segP.set_p_x(p_x_alloc);
	        segP.set_p_y(p_y_alloc);
	        segP.set_theta(Constants.THETA);
	        segP.set_gScript(segU);*/
	        
	        //Run calculations
	        Allocation scribbleAllocBG = Allocation.createFromBitmap(mRS, pathbitmapbg, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        Allocation scribbleAllocFG = Allocation.createFromBitmap(mRS, pathbitmapfg, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        projectionToConstraint.set_initialized(1);
	        for (int i = 0; i < iterations; i++) {
		        segU.invoke_filter();
		        segP.invoke_filter();
		        //Projection to constraint
		        projectionToConstraint.set_gIn(scribbleAllocBG);
		        projectionToConstraint.set_foreground(0);
		        projectionToConstraint.invoke_filter();
		        projectionToConstraint.set_gIn(scribbleAllocFG);
		        projectionToConstraint.set_foreground(1);
		        projectionToConstraint.invoke_filter();
	        }
	        
	        //Copy result from allocation to array
	        uAllocation.copyTo(u);
	        
	        //Free unused resources
	        scribbleAllocBG.destroy();
	        scribbleAllocFG.destroy();
	        imgGradAlloc.destroy();
	        
	        imageGradient.recycle();
	        imageGradient = null;
	        
	        //Create monochrome picture
	        Bitmap monochrome = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
	        Allocation monochromeAlloc = Allocation.createFromBitmap(mRS, monochrome, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        ScriptC_createMonochrome createmonochrome = new ScriptC_createMonochrome(mRS);
	        createmonochrome.set_gIn(uAllocation);
	        createmonochrome.set_gOut(monochromeAlloc);
	        createmonochrome.set_threshold(Constants.CONTOUR_THRESHOLD);
	        createmonochrome.set_gScript(createmonochrome);
	        createmonochrome.invoke_filter();
	        monochromeAlloc.copyTo(monochrome);
	        
	        //Clean up monochrome allocation
	        monochromeAlloc.destroy();
	        
	        //Check results
	        /*for (int i = 0; i < u.length; i++) {
	        	Log.v("AppDebug", "U: " + u[i]);
	        }*/
	        /*for (int i = 0; i < bmp.getWidth(); i++) {
	        	for (int j = 0; j < bmp.getHeight(); j++) {
	        		bmp.setPixel(i, j, Color.rgb((int)(u[j * bmp.getWidth() + i] * 255), 0, 0));
	        	}
	        }*/
	        
	        //Draw contours
	        Allocation bmpAlloc = Allocation.createFromBitmap(mRS, bmp, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
	        ScriptC_drawContours drawcontours = new ScriptC_drawContours(mRS);
	        drawcontours.set_contourWidth((int)(Constants.CONTOUR_WIDTH_RATIO * bmp.getWidth()));
	        drawcontours.set_threshold(Constants.CONTOUR_THRESHOLD);
	        drawcontours.set_image(bmpAlloc);
	        drawcontours.set_u(uAllocation);
	        drawcontours.set_gScript(drawcontours);
	        drawcontours.invoke_filter();
	        
	        bmpAlloc.copyTo(bmp);
	        
	        //Clean bmp Allocation
	        bmpAlloc.destroy();
	     	  
	        //displayU(monochrome);
	        displayU(bmp);
	        
	        //Free U allocation
	        uAllocation.destroy();
	        
	        dismissProgress();
		}
	}

	/**
	 * This method saves the processed image with drawn contours and the
	 * u-matrix as a binary image to the internal storage of the device
	 */
	private void saveImage() {
		if (((TouchImageView) findViewById(R.id.imageView1)).getDrawable() != null
				&& ((TouchImageView) findViewById(R.id.imageView1))
						.getDrawable() instanceof BitmapDrawable
				&& ((BitmapDrawable) ((TouchImageView) findViewById(R.id.imageView1))
						.getDrawable()).getBitmap() != null
				&& ((ImageView) findViewById(R.id.imageView2)).getDrawable() != null
				&& ((ImageView) findViewById(R.id.imageView2)).getDrawable() instanceof BitmapDrawable
				&& ((BitmapDrawable) ((ImageView) findViewById(R.id.imageView2))
						.getDrawable()).getBitmap() != null) {
			Bitmap bmp_contour = ((BitmapDrawable) ((TouchImageView) findViewById(R.id.imageView1))
					.getDrawable()).getBitmap();

			Bitmap rgb_binary = ((BitmapDrawable) ((ImageView) findViewById(R.id.imageView2))
					.getDrawable()).getBitmap();

			// Project to monochrome image


			String outPath_contour = getPathWithoutFilename(imageURI)
					+ File.separator + getFilename(imageURI, false)
					+ "_segmented.png";
			String outPath_binary = getPathWithoutFilename(imageURI)
					+ File.separator + getFilename(imageURI, false)
					+ "_binary.png";

			OutputStream os = null;
			File file_contour = new File(outPath_contour);
			File file_binary = new File(outPath_binary);
			try {
				os = new FileOutputStream(file_contour);
				bmp_contour.compress(Bitmap.CompressFormat.PNG, 100, os);
				try {
					os.flush();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				try {
					os.close();
					MediaStore.Images.Media.insertImage(getContentResolver(),
							file_contour.getAbsolutePath(),
							file_contour.getName(), file_contour.getName());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}

			try {

				os = new FileOutputStream(file_binary);
				rgb_binary.compress(Bitmap.CompressFormat.PNG, 100, os);
				try {
					os.flush();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				try {
					os.close();
					MediaStore.Images.Media.insertImage(getContentResolver(),
							file_binary.getAbsolutePath(),
							file_binary.getName(), file_binary.getName());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * A fragment containing the main view.
	 * 
	 * @author Magdalena Neumann
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}

		/**
		 * Returns a new instance of the PlaceholderFragment class
		 * 
		 * @return
		 */
		public static Fragment newInstance() {
			PlaceholderFragment firstInstance = new PlaceholderFragment();
			return firstInstance;
		}
	}

	/**
	 * A fragment containing the view for displaying the u-matix
	 * 
	 * @author Magdalena Neumann
	 * 
	 */
	public static class UFragment extends Fragment {

		public static UFragment newInstance() {
			UFragment firstInstance = new UFragment();
			return firstInstance;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_u, container, false);
			return view;
		}
	}

	/**
	 * A fragment containing the view for displaying the settings page
	 * 
	 * @author Magdalena Neumann
	 * 
	 */
	public static class SettingsFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener {

		public static SettingsFragment newInstance() {
			SettingsFragment firstInstance = new SettingsFragment();
			return firstInstance;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.layout.preferences);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			// Set new foreground color
			if (key.equals("pref_color_fg")) {
				if (this.getActivity() instanceof MainActivity) {
					int color = sharedPreferences.getInt("pref_color_fg",
							0xff000000);
					((TouchImageView) this.getActivity().findViewById(
							R.id.imageView1)).getForegroundPaths().getPaint()
							.setColor(color);
					((TouchImageView) this.getActivity().findViewById(
							R.id.imageView1)).invalidate();
				}
			}
			// Set new background color
			if (key.equals("pref_color_bg")) {
				if (this.getActivity() instanceof MainActivity) {
					int color = sharedPreferences.getInt("pref_color_bg",
							0xffffffff);
					((TouchImageView) this.getActivity().findViewById(
							R.id.imageView1)).getBackgroundPaths().getPaint()
							.setColor(color);
					((TouchImageView) this.getActivity().findViewById(
							R.id.imageView1)).invalidate();
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onResume() {
			super.onResume();
			getPreferenceManager().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);

		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onPause() {
			getPreferenceManager().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public static class MyPagerAdapter extends FragmentPagerAdapter {
		private static int NUM_PAGES = 3;

		public MyPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getCount() {
			return NUM_PAGES;
		}

		// Returns the fragment to display
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return SettingsFragment.newInstance();
			case 1:
				return PlaceholderFragment.newInstance();
			case 2:
				return UFragment.newInstance();
			default:
				return null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return "Settings";
			case 1:
				return "Contour";
			case 2:
				return "U";
			default:
				return "";
			}
		}

	}

	/**
	 * This method displays the chosen image on the imageview
	 * 
	 * @param image
	 *            Image to be displayed
	 * @param resetPaths
	 *            Flag to determine whether or not to reset the loaded scribbles
	 */
	public void displayImage(Bitmap image, boolean resetPaths) {
		TouchImageView imgview = (TouchImageView) findViewById(R.id.imageView1);
		if (imgview != null
				&& imgview.getDrawable() != null
				&& imgview.getDrawable() instanceof BitmapDrawable
				&& image != ((BitmapDrawable) (imgview.getDrawable()))
						.getBitmap()
				&& !((BitmapDrawable) (imgview.getDrawable())).getBitmap()
						.isRecycled()) {
			((BitmapDrawable) (imgview.getDrawable())).getBitmap().recycle();
		}
		System.gc();
		imgview.setImageBitmap(image);
		if (resetPaths)
			imgview.clearDrawingPaths();
	}

	/**
	 * This method displays the variable u on the U imageview
	 * 
	 * @param u
	 *            Matrix to be displayed
	 */
	public void displayU(Bitmap u) {
		ImageView imgview = (ImageView) findViewById(R.id.imageView2);
		if (imgview != null
				&& imgview.getDrawable() != null
				&& imgview.getDrawable() instanceof BitmapDrawable
				&& u != ((BitmapDrawable) (imgview.getDrawable())).getBitmap()
				&& !((BitmapDrawable) (imgview.getDrawable())).getBitmap()
						.isRecycled()) {
			((BitmapDrawable) (imgview.getDrawable())).getBitmap().recycle();
		}
		System.gc();
		imgview.setImageBitmap(u);
		imgview.setVisibility(View.VISIBLE);
	}

	/**
	 * This method opens the gallery
	 */
	public void openGallery(int ID) {
		Intent galleryIntent = new Intent(Intent.ACTION_PICK);
		galleryIntent.setType("image/*");
		startActivityForResult(galleryIntent, ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		if (requestCode == Constants.GALLERY_IMAGE_ID) {
			if (resultCode == RESULT_OK) {
				if (findViewById(R.id.imageView2) != null
						&& findViewById(R.id.imageView2) instanceof ImageView) {
					((ImageView) findViewById(R.id.imageView2))
							.setVisibility(View.GONE);
				}
				imageURI = imageReturnedIntent.getData();

				reloadImage(imageURI, true);
				actionBar.findItem(R.id.action_start_segmentation).setVisible(
						true);
				findViewById(R.id.imageView1).setVisibility(View.VISIBLE);
				actionBar.findItem(R.id.action_save_pic).setVisible(false);
			}
		}

		if (requestCode == Constants.GALLERY_SCRIBBLE_ID) {
			if (resultCode == RESULT_OK) {
				scribbleURI = imageReturnedIntent.getData();
				((TouchImageView) findViewById(R.id.imageView1))
						.clearDrawingPaths();
				((TouchImageView) findViewById(R.id.imageView1))
						.setDrawMode(false);
			}
		}
	}

	/**
	 * Determines the full path of a file including the filename
	 * 
	 * @param uri
	 *            File described by its URI
	 * @return Full path of the file
	 */
	private String getPathWithFilename(Uri uri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		@SuppressWarnings("deprecation")
		Cursor cursor = managedQuery(uri, proj, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	/**
	 * Determines the full path of a file without the filename
	 * 
	 * @param uri
	 *            File described by its URI
	 * @return Full path of the file without filename
	 */
	private String getPathWithoutFilename(Uri uri) {
		File f = new File(getPathWithFilename(uri));
		String absPath = f.getAbsolutePath();
		return absPath.substring(0, absPath.lastIndexOf(File.separator));
	}

	/**
	 * Determines the filename of a file
	 * 
	 * @param uri
	 *            File described by its URI
	 * @return Filename of the file
	 */
	private String getFilename(Uri uri, boolean extension) {
		File f = new File(getPathWithFilename(uri));
		if (extension) {
			return f.getName();
		} else {
			String name = f.getName();
			return name.substring(0, name.lastIndexOf("."));
		}
	}

	/**
	 * Reloads the selected image from the storage of the device
	 * 
	 * @param uri
	 *            URI of the selected image
	 * @param updateView
	 *            Flag to determine whether or not to update the ImageView
	 */
	private void reloadImage(Uri uri, boolean updateView) {

		if (image_original != null && !image_original.isRecycled()) {
			image_original.recycle();
		}
		if (image_scaled != null && !image_scaled.isRecycled()) {
			image_scaled.recycle();
		}
		System.gc();
		InputStream inStream;
		try {
			// Decode Image size
			inStream = getContentResolver().openInputStream(uri);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(inStream, null, options);

			// Save image scaling status
			// Divide available memory by 48 to leave space fore all
			// bytebuffers that are used to process the image later
			// This value leaves some free space for varialbes etc.

			long maxNumPixels = (Runtime.getRuntime().maxMemory()
					- Runtime.getRuntime().totalMemory() + Runtime.getRuntime()
					.freeMemory()) / 48;

			long userMaxPixels = (getMaxNumPixels() > 0 ? (long) getMaxNumPixels()
					: maxNumPixels);
			if (userMaxPixels > 0
					&& options.outWidth * options.outHeight > userMaxPixels) {
				scaled = true;
			} else {
				scaled = false;
			}
			
			scaled_height = options.outHeight;
			scaled_width = options.outWidth;

			if (maxNumPixels > userMaxPixels) {
				maxNumPixels = userMaxPixels;
			}

			int scale = 1;

			while ((options.outWidth * options.outHeight)
					* (1 / Math.pow(scale, 2)) > maxNumPixels) {
				scale++;
			}

			try {
				inStream = getContentResolver().openInputStream(uri);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			if (scale > 1) {
				scale--;
				options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				options.inSampleSize = scale;
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;

				image_scaled = BitmapFactory.decodeStream(inStream, null,
						options);

				// resize to desired dimensions
				int height = image_scaled.getHeight();
				int width = image_scaled.getWidth();

				double y = Math
						.sqrt(maxNumPixels / (((double) width) / height));
				double x = (y / height) * width;

				scaled_height = (int) y;
				scaled_width = (int) x;

				Bitmap scaledBitmap = Bitmap.createScaledBitmap(image_scaled,
						(int) x, (int) y, true);
				image_scaled.recycle();
				image_scaled = scaledBitmap;

				System.gc();
			} else {
				image_scaled = BitmapFactory.decodeStream(inStream);
			}

			inStream = getContentResolver().openInputStream(imageURI);
			image_original = BitmapFactory.decodeStream(inStream);

			// Convert image to ARGB_8888
			if (image_scaled.getConfig() != Bitmap.Config.ARGB_8888) {
				image_scaled = image_scaled.copy(Bitmap.Config.ARGB_8888, true);
			}

			if (image_original.getConfig() != Bitmap.Config.ARGB_8888) {
				image_original = image_original.copy(Bitmap.Config.ARGB_8888,
						true);
			}
			if (updateView) {
				displayImage(image_original, true);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Function to translate the number of pixels for calculation determined in
	 * the settings page into integer values
	 * 
	 * @return Maximum number of pixels of the image for calculation
	 */
	private int getMaxNumPixels() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String selection = sp.getString("pref_scale", "7");

		// Has to be converted to int since switch is not implemented for type
		// string
		int result = Integer.parseInt(selection);
		switch (result) {
		case 1:
			return -1;
		case 2:
			return 3000000;
		case 3:
			return 1000000;
		case 4:
			return 500000;
		case 5:
			return 200000;
		case 6:
			return 100000;
		case 7:
			return 50000;
		case 8:
			return 10000;
		default:
			return 50000;
		}
	}

	/**
	 * Reads scribbles from the internal storage of the device into a Bitmap
	 * 
	 * @return Bitmap containing foreground and background scribbles
	 */
	private Bitmap readScribbleBitmap() {

		Bitmap bmp;
		InputStream inStream;
		long maxNumPixels = image_scaled.getWidth() * image_scaled.getHeight();

		// Decode Image size
		try {
			inStream = getContentResolver().openInputStream(scribbleURI);
			if (scaled) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(inStream, null, options);

				int scale = 1;

				while ((options.outWidth * options.outHeight)
						* (1 / Math.pow(scale, 2)) > maxNumPixels) {
					scale++;
				}

				inStream = getContentResolver().openInputStream(scribbleURI);

				// Create scaled bitmap
				scale--;
				options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				options.inSampleSize = scale;
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;

				bmp = BitmapFactory.decodeStream(inStream, null, options);

				// Resize to desired dimensions
				Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp,
						scaled_width, scaled_height, true);
				bmp.recycle();
				bmp = scaledBitmap;

				System.gc();
			} else {
				bmp = BitmapFactory.decodeStream(inStream);
			}

			if (bmp.getConfig() != Bitmap.Config.ARGB_8888) {
				bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
			}

			return bmp;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Reads either the foreground or background scribbles from the storage of
	 * the device into a Bitmap
	 * 
	 * @param fg
	 *            Flag to determine whether to isolate the foreground or
	 *            background scribbles
	 * @return Bitmap containing the desired scribbles
	 */
	private Bitmap getScribbleFGBG(boolean fg) {
		//Bitmap fgbg = Bitmap.createBitmap(scaled_width, scaled_height,
		//		Config.ALPHA_8);
		Bitmap fgbg = Bitmap.createBitmap(scaled_width, scaled_height,
				Config.ARGB_8888);
		Bitmap scribbleBmp = readScribbleBitmap();

		//Read scribbles
		ScriptC_createFGBG createfgbg = new ScriptC_createFGBG(mRS);
		
		//Create allocations
		Allocation fgbgAlloc = Allocation.createFromBitmap(mRS, fgbg, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
		Allocation scribbleBmpAlloc = Allocation.createFromBitmap(mRS, scribbleBmp, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);

		//Bind allocations
		createfgbg.set_gIn(scribbleBmpAlloc);
		createfgbg.set_gOut(fgbgAlloc);
		createfgbg.set_gScript(createfgbg);

		if (fg) {
			//Bind foreground colors
			createfgbg.set_blueValue(Constants.SCRIBBLE_BLUE_FG);
			createfgbg.set_greenValue(Constants.SCRIBBLE_GREEN_FG);
			createfgbg.set_redValue(Constants.SCRIBBLE_RED_FG);			
		} else {
			//Bind background colors
			createfgbg.set_blueValue(Constants.SCRIBBLE_BLUE_BG);
			createfgbg.set_greenValue(Constants.SCRIBBLE_GREEN_BG);
			createfgbg.set_redValue(Constants.SCRIBBLE_RED_BG);
		}
		
		//Run calculation
		createfgbg.invoke_filter();
		
		//Read data from allocation
		fgbgAlloc.copyTo(fgbg);
		
		//Clean up		
		scribbleBmp.recycle();
		fgbgAlloc.destroy();
		scribbleBmpAlloc.destroy();

		return fgbg;
	}

	/**
	 * This class models a thread for executing the segmentation algorithm
	 * 
	 * @author Magdalena Neumann. Updated By Sebastian Soyer.
	 * 
	 */
	public class SegmentationThread extends AsyncTask<Void, Void, ByteBuffer> {

		ByteBuffer imageGradientBuffer;
		ByteBuffer pathbitmapfgBuffer;
		ByteBuffer pathbitmapbgBuffer;
		int height;
		int width;
		int iterations;
		double alpha;
		double beta;
		double tau;
		double theta;

		public SegmentationThread(ByteBuffer imageGradientBuffer,
				ByteBuffer pathbitmapfgBuffer, ByteBuffer pathbitmapbgBuffer,
				int height, int width, int iterations, double alpha,
				double beta, double tau, double theta) {
			this.imageGradientBuffer = imageGradientBuffer;
			this.pathbitmapfgBuffer = pathbitmapfgBuffer;
			this.pathbitmapbgBuffer = pathbitmapbgBuffer;
			this.height = height;
			this.width = width;
			this.iterations = iterations;
			this.alpha = alpha;
			this.beta = beta;
			this.tau = tau;
			this.theta = theta;

		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected ByteBuffer doInBackground(Void... arg0) {
			// Temporarily disable screen rotation
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

			return imageGradientBuffer;
			// return pathbitmapfgBuffer;
		}

		/**
		 * This method is executed after the thread has finished execution
		 * 
		 * @param returnBuffer
		 *            Buffer returned by the segmentation thread
		 */
		protected void onPostExecute(ByteBuffer returnBuffer) {
			callbackSegmentation(returnBuffer);

			// imageGradientBuffer is closed in DrawContours thread
			pathbitmapbgBuffer = null;
			pathbitmapfgBuffer = null;
			MainActivity.this.pathbitmapbgBuffer = null;
			MainActivity.this.pathbitmapfgBuffer = null;

			// Enable screen rotation
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
	}

	/**
	 * This class models a thread to draw the calculated contours on the original
	 * bitmaps
	 * 
	 * @author Magdalena Neumann
	 * 
	 */
	public class DrawContoursThread extends AsyncTask<Void, Void, ByteBuffer> {

		ByteBuffer imageBuffer;
		ByteBuffer u_ref;
		int height;
		int width;
		double contourThreshold;
		int contourWidth;

		public DrawContoursThread(ByteBuffer imageBuffer, ByteBuffer u_ref,
				int height, int width, double contourThreshold, int contourWidth) {
			this.imageBuffer = imageBuffer;
			this.u_ref = u_ref;
			this.height = height;
			this.width = width;
			this.contourThreshold = contourThreshold;
			this.contourWidth = contourWidth;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected ByteBuffer doInBackground(Void... arg0) {
			return null;
		}

		/**
		 * This method is executed after the thread has finished execution
		 * 
		 * @param returnBuffer
		 *            Buffer returned by the draw contour thread
		 */
		protected void onPostExecute(ByteBuffer returnBuffer) {
			callbackDrawContours(returnBuffer);

			dismissProgress();
		}

	}
}
