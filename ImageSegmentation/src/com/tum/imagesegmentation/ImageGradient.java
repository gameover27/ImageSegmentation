package com.tum.imagesegmentation;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Type;

public class ImageGradient {
	private Bitmap input;
	private RenderScript mRS;

	public ImageGradient(Bitmap input, RenderScript mRS) {
		this.input = input;
		this.mRS = mRS;
	}
	
	public Bitmap getBitmapSobelApplied() {
		//Generate grayscale image
		ScriptC_createGrayscale creategrayscale= new ScriptC_createGrayscale(mRS);
		Allocation inputAlloc = Allocation.createFromBitmap(mRS, input, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
		Allocation grayscaleAlloc = Allocation.createTyped(mRS, inputAlloc.getType());
		creategrayscale.set_gIn(inputAlloc);
		creategrayscale.set_gOut(grayscaleAlloc);
		creategrayscale.set_gScript(creategrayscale);
		creategrayscale.invoke_filter();
		
		//Free input allocation
		inputAlloc.destroy();
		
		//Create allocations for x and y derivatives
		Type sobelType = new Type.Builder(mRS, Element.F32_3(mRS)).setX(input.getWidth()).setY(input.getHeight()).create();
		Allocation sobelXAlloc = Allocation.createTyped(mRS, sobelType);
		Allocation sobelYAlloc = Allocation.createTyped(mRS, sobelType);
		
		//Create allocation for sobel convolution kernel
		Type kernelType = new Type.Builder(mRS, Element.F32(mRS)).setX(Constants.sobelKernelWidth).setY(Constants.sobelKernelHeight).create();
		Allocation kernelAlloc = Allocation.createTyped(mRS, kernelType);
        kernelAlloc.copy2DRangeFrom(0, 0, kernelType.getX(), kernelType.getY(), Constants.sobelKernelX);
		
        //Apply sobel filter
        ScriptC_filterImage filterimage = new ScriptC_filterImage(mRS);
        filterimage.set_filterMatrix(kernelAlloc);
        filterimage.set_gIn(grayscaleAlloc);
        filterimage.set_gOut(sobelXAlloc);
        filterimage.set_gScript(filterimage);
        filterimage.invoke_filter();
        
        kernelAlloc.copy2DRangeFrom(0, 0, kernelType.getX(), kernelType.getY(), Constants.sobelKernelY);
		filterimage.set_filterMatrix(kernelAlloc);
		filterimage.set_gOut(sobelYAlloc);
		filterimage.invoke_filter();
		
		//Free allocation for grayscale image
		grayscaleAlloc.destroy();
		
		//Combine x and y derivatives
		Allocation imageGradientAlloc = Allocation.createFromBitmap(mRS, input, Allocation.MipmapControl.MIPMAP_NONE,Allocation.USAGE_SCRIPT);
		ScriptC_combineXY combinexy = new ScriptC_combineXY(mRS);
		combinexy.set_gX(sobelXAlloc);
		combinexy.set_gY(sobelYAlloc);
		combinexy.set_gOut(imageGradientAlloc);
		combinexy.set_gScript(combinexy);
		combinexy.invoke_filter();
		
		//Free x and y derivatives and kernel
		sobelXAlloc.destroy();
		sobelYAlloc.destroy();
		kernelAlloc.destroy();
		
		//Create output image
		Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Config.ARGB_8888);
		imageGradientAlloc.copyTo(result);
		
		//Free image gradient allocation
		imageGradientAlloc.destroy();
		
		return result;
	}

	public Bitmap getInput() {
		return input;
	}

	public void setInput(Bitmap input) {
		this.input = input;
	}
}
