/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <float.h>
#include <omp.h>


#define LOG_TAG "JNI"

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "AppDebugNDK", __VA_ARGS__) 
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "AppDebugNDK", __VA_ARGS__) 
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "AppDebugNDK", __VA_ARGS__) 
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "AppDebugNDK", __VA_ARGS__) 
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "AppDebugNDK", __VA_ARGS__)

#define MAX(a,b) ((a) > (b) ? a : b)
#define MIN(a,b) ((a) < (b) ? a : b)

//2D Gradient
struct gradientXY {
  double x;
  double y;
};

int counter = 0;
int h = 0;

struct pixelrgba {
int red;
int green;
int blue;
int alpha;
};

void readPixelColor(uint32_t pixel, struct pixelrgba * pixelcolor) {
  pixelcolor->red = (int) (pixel & 0x00000FF );
  pixelcolor->green = (int)((pixel & 0x0000FF00) >> 8);
  pixelcolor->blue = (int) ((pixel & 0x00FF0000) >> 16);
  pixelcolor->alpha = (int) (pixel & 0xFF000000) >> 24;
}
void setPixelColor(uint32_t* pixel, struct pixelrgba * pixelcolor) {
  *pixel =
    ((pixelcolor->alpha << 24) & 0xFF000000) |
    ((pixelcolor->blue << 16) & 0x00FF0000) |
    ((pixelcolor->green << 8) & 0x0000FF00) |
    (pixelcolor->red & 0x000000FF);
}

//Right/Leftsided difference quotient
double derivativeLeftRight(double left, double right, double distance) {
    return (right - left) / distance;
}

//Central difference quotient
double derivativeCentral(double left, double right, double distance) {
    return (right - left) / (2.0 * distance);
}

//Edgedetection function g
double g(double alpha, double beta, uint8_t * imageGradient, int width) {
  double ig = (*imageGradient) / 255.0;
  double result = exp(pow(fabs(ig), beta) * -1.0 * alpha);
  
  //Always return values greater 0 (due to machine accuracy)
  if (result == 0.0) {
    result = DBL_MIN;
  }
  return result;
}

//Calculates u for one pixel 
void calculateU(double* u, double* p_x, double* p_y, double theta, int xx, int yy, int line, int width, int height) {
  //Calculate u^(n+1)
  int line_xx = line + xx;
  double deriv_x = p_x[line_xx];
  double deriv_y = p_y[line_xx];
  //LeftRight
  if (xx != 0) {
    deriv_x -= p_x[line_xx-1];
  } 
  if (yy != 0) {
    deriv_y -= p_y[line_xx-width];
  }
  
  u[line_xx] += theta * (deriv_x + deriv_y);
    
  //Ensure that bounds are respected
  if (u[line_xx] > 1.0)
    u[line_xx] = 1.0;
  else if (u[line_xx] < 0.0)
    u[line_xx] = 0.0;
}

//Calculates p for one pixel
void calculateP(uint8_t* pixels_bmp, double* p_x, double* p_y,
		double* u, double alpha, double beta, double tau,
		double theta, int xx, int yy, int line,
		int width, int height) {
  double p_tilde_x, p_tilde_y;
  
  int line_p_xx = line + xx;
  double grad_x;
  double grad_y;
  
  if (xx == width-1) {
    grad_x = 0;
  } else {
    grad_x = u[line_p_xx+1];
    grad_x -= u[line_p_xx];
  }
  if (yy == height-1) {
    grad_y = 0;
  } else {
    grad_y = u[line_p_xx+width];
    grad_y -= u[line_p_xx];
  }
  
  p_tilde_x = p_x[line_p_xx] + (tau / theta) * grad_x;
  p_tilde_y = p_y[line_p_xx] + (tau / theta) * grad_y;
  
  double norm_term = fmax(1.0, (sqrt(pow(p_tilde_x, 2.0) + pow(p_tilde_y, 2.0)) /
			      g(alpha, beta, &(pixels_bmp[line_p_xx]), width)));
  p_x[line_p_xx] = p_tilde_x / norm_term;
  p_y[line_p_xx] = p_tilde_y / norm_term;
}

void writeOutput(double * u, uint8_t * u_out, uint8_t * contour_out, int height, int width, double contourThreshold) {
  int xx;
  int yy;
  int line;
  
  // Write u;
  #pragma omp parallel for private(line)
  for(yy = 0; yy < height; yy++) {
    line = yy * width;
    #pragma omp parallel for
    for(xx = 0; xx < width; xx++) {
      if (u[line + xx] >= contourThreshold) {
	u_out[line + xx] = 255;
      } else {
	u_out[line + xx] = 0;
      }
      //to output u as a grayscale image uncomment the following line
      //u_out[line + xx] = MIN(MAX(0,(uint8_t)(u[line + xx] * 255)),255);
    }
  }
}

/* 
 * This method is the image segmentation algorithm written in native language C. It is invoked by the MainActivity in the ImageSegmentation project.
 */
JNIEXPORT void JNICALL
Java_com_tum_imagesegmentation_MainActivity_performSegmentationJNI( JNIEnv* env,
                                                  jobject thiz, jobject bitmapBuffer, jobject fgBuffer, jobject bgBuffer, jint height, jint width, jint iterations, jdouble alpha, jdouble beta, jdouble tau, jdouble theta, jdouble contourThreshold)
{
  
  uint8_t* pixels_pathfg;
  uint8_t* pixels_pathbg;
  uint8_t* pixels_bmp;
  
  int height_nat = (int)height;
  int width_nat = (int)width;
  int line;
  
  int i, xx, yy;
  double* u = malloc((height_nat * width_nat)*sizeof(double));
  double* p_x = malloc((height_nat * width_nat)*sizeof(double));
  double* p_y = malloc((height_nat * width_nat)*sizeof(double));
  
  pixels_pathfg = (uint8_t*) (*env)->GetDirectBufferAddress(env, fgBuffer);
  pixels_pathbg = (uint8_t*) (*env)->GetDirectBufferAddress(env, bgBuffer);
  pixels_bmp = (uint8_t*) (*env)->GetDirectBufferAddress(env, bitmapBuffer);
  
  //Updates the progress bar 
  jclass clazz = (*env)->FindClass(env, "com/tum/imagesegmentation/MainActivity");
  jmethodID incProg = (*env)->GetMethodID(env, clazz, "incrementProgress", "()V");
  
  //Initialize arrays
  for(yy = 0; yy < height_nat; yy++) {
    line = yy * width_nat;
    for(xx = 0; xx < width_nat; xx++) {
      //Initialize u
      if (pixels_pathfg[line + xx] == 42) {
	//Set u = 1 for foreground
	u[line + xx] = 1.0;
      } else if (pixels_pathbg[line + xx] == 42) {
	//Set u = 0 for background
	u[line + xx] = 0.0;
      } else {
	//Set u = 0.5 for neutral pixels
	u[line + xx] = 0.5;
      }
      
      //Initialize p
      p_x[line + xx] = 0.0;
      p_y[line + xx] = 0.0;
    }
  }
  
  // Iterate algorithm
  for (i = 0; i < (int)iterations; i++) {
    // Calculate p_tilde and p^n+1 
    #pragma omp parallel for private(line)
    for(yy = 0; yy < height_nat; yy++) {
      line = yy * width_nat;
      //LOGE("Number of threads: %d", thr);
      #pragma omp parallel for
      for(xx = 0; xx < width_nat ; xx++) {
	  calculateP(pixels_bmp, p_x, p_y, u, (double)alpha, (double)beta, (double)tau, (double)theta, xx, yy, line, width_nat, height_nat);
      }
    }
    
    
    //Calcualte U
    #pragma omp parallel for private(line)
    for(yy = 0; yy < height_nat; yy++) {
      line = yy * width_nat;
      #pragma omp parallel for
      for(xx = 0; xx < width_nat; xx++) {
	calculateU(u, p_x, p_y, (double)theta, xx, yy, line, width_nat, height_nat);
	
	// Projection to constraint
	if (pixels_pathfg[line + xx] == 42) {
	  //Set u = 1 for foreground
	  u[line + xx] = 1.0;
	} else if (pixels_pathbg[line + xx] == 42) {
	  //Set u = 0 for background
	  u[line + xx] = 0.0;
	}
      }
    }
      (*env)->CallVoidMethod(env, thiz, incProg);
  }
  
  writeOutput(u, pixels_bmp, pixels_pathfg, height_nat, width_nat, contourThreshold);
  
  //Finally free allocated memory
  free(u);
  free(p_x);
  free(p_y);
}

/* 
 * This method creates a binary image from a grayscale image.  
 */
JNIEXPORT void JNICALL 
Java_com_tum_imagesegmentation_MainActivity_createMonochrome(JNIEnv* env, jobject thiz, jobject imageBuffer, jint height, jint width, jdouble contourThreshold) {
  uint32_t * image;
  image = (uint32_t*) (*env)->GetDirectBufferAddress(env, imageBuffer);
  
  int xx;
  int yy;
  int line;
  
  double luminosity;
  
  struct pixelrgba * pixelcolor = malloc(sizeof(struct pixelrgba));
  struct pixelrgba * pixelcolor_black = malloc(sizeof(struct pixelrgba));
  struct pixelrgba * pixelcolor_white = malloc(sizeof(struct pixelrgba));
  
  pixelcolor_black->red = 0;
  pixelcolor_black->blue = 0;
  pixelcolor_black->green = 0;
  pixelcolor_black->alpha = 255;
  
  pixelcolor_white->red = 255;
  pixelcolor_white->blue = 255;
  pixelcolor_white->green = 255;
  pixelcolor_white->alpha = 255;
  
  
  // Project to monochrome
  for(yy = 0; yy < height; yy++) {
    line = yy * width;
    for(xx = 0; xx < width; xx++) {
      readPixelColor(image[line+xx], pixelcolor);
      luminosity = 0.2126 * pixelcolor->red + 0.7152 * pixelcolor->green + 0.0722 * pixelcolor->blue;
      if (luminosity >= contourThreshold * 255) {
	//Using another color than pixelcolor_white will set the foreground as the new color instead of white color
	setPixelColor(&image[line + xx], pixelcolor_white);
      } else {
	//Using another color than pixelcolor_black will set the background as the new color instead of black color
	setPixelColor(&image[line + xx], pixelcolor_black);
      }
    }
  }
  
  //Finally free allocated space
  free(pixelcolor);
  free(pixelcolor_black);
  free(pixelcolor_white);
}

/* 
 * This method allocates memory on the native heap.
 */
JNIEXPORT jobject JNICALL Java_com_tum_imagesegmentation_MainActivity_allocNative(JNIEnv* env, jobject thiz, jlong size)
{
  void* buffer = malloc(size);
  jobject directByteBuffer = (*env)->NewDirectByteBuffer(env, buffer, size);
  return directByteBuffer;
}

/* 
 * This method frees memory on the native heap. 
 */
JNIEXPORT void JNICALL Java_com_tum_imagesegmentation_MainActivity_freeNative(JNIEnv* env, jobject thiz, jobject bufferAddr)
{
  void *buffer = (*env)->GetDirectBufferAddress(env, bufferAddr);
  free(buffer);
}

/* 
 * This method reads given scribbles from an image.  
 */
JNIEXPORT void JNICALL Java_com_tum_imagesegmentation_MainActivity_readScribbleJNI(JNIEnv* env, jobject thiz, jobject scribbleBuffer, jobject fgbgBuffer, jint height, jint width, jint redValue, jint greenValue, jint blueValue)
{
  int xx;
  int yy;
  int line;
  
  uint32_t * scribble;
  uint8_t * fgbg;
  
  scribble = (uint32_t*) (*env)->GetDirectBufferAddress(env, scribbleBuffer);
  fgbg = (uint8_t*) (*env)->GetDirectBufferAddress(env, fgbgBuffer);
  
  struct pixelrgba * pixelcolor = malloc(sizeof(struct pixelrgba));
  
  //goes through every pixel and marks in the passed resulting image the corresponding pixels on accordance 
  #pragma omp parallel for private(line)
  for(yy = 0; yy < height; yy++) {
    line = yy * width;
    #pragma omp parallel for
    for(xx = 0; xx < width; xx++) {
      readPixelColor(scribble[line+xx], pixelcolor);
      if (pixelcolor->red == redValue && pixelcolor->green == greenValue && pixelcolor->blue == blueValue) {
	fgbg[line+xx] = 42;
      } else {
	fgbg[line+xx] = 0;
      }
    }
  }
  
  //Finally free allocated memory
  free(pixelcolor);
}

/* 
 * This method draws a contour on the image. 
 */
JNIEXPORT void JNICALL Java_com_tum_imagesegmentation_MainActivity_drawContoursJNI(JNIEnv* env, jobject thiz, jobject imageBuffer, jobject u_ref, jint height, jint width, jdouble contourThreshold, jint contourWidth)
{
  uint32_t * image;
  uint8_t * u;
  
  int xx;
  int yy;
  int x_paint;
  int y_paint;
  
  image = (uint32_t*) (*env)->GetDirectBufferAddress(env, imageBuffer);
  u = (uint8_t*) (*env)->GetDirectBufferAddress(env, u_ref);
  
  jclass clazz = (*env)->FindClass(env, "com/tum/imagesegmentation/MainActivity");
  jmethodID incProg = (*env)->GetMethodID(env, clazz, "incrementProgress", "()V");
  
  struct pixelrgba * pixelcolor = malloc(sizeof(struct pixelrgba));
  
  int yy_top = 0;
  int xx_left = 0;
  
  // Sets the color for the contour
  pixelcolor->red = 255;
  pixelcolor->blue = 0;
  pixelcolor->green = 0;
  
  // Traverse the image once and detect transition between fore- and background
  for(yy = yy_top; yy < (int)height-1; yy++) {
    for(xx = xx_left; xx < (int)width-1; xx++) {
      if (u[yy*(int)width + xx] >= contourThreshold * 255 &&
	      MIN(MIN(MIN(MIN(MIN(MIN(MIN(MIN(u[(yy-1)*(int)width + xx], u[(yy)*(int)width + xx]), u[(yy+1)*(int)width + xx]), u[(yy-1)*(int)width + (xx-1)]), u[(yy-1)*(int)width + (xx+1)]),u[(yy+1)*(int)width + (xx-1)]), u[(yy+1)*(int)width + (xx+1)]), u[yy*(int)width + (xx-1)]), u[yy*(int)width + (xx+1)]) < contourThreshold * 255) {
	if ((int)contourWidth > 1) {
	  for(y_paint = 0; y_paint < (int)contourWidth && (yy - ((int)(contourWidth / 2.0)) + y_paint) < height; y_paint++) {
	    for(x_paint = 0; x_paint < (int)contourWidth && (xx - ((int)(contourWidth / 2.0)) + x_paint) < width; x_paint++) {
	      if(((int)((int)contourWidth / 2.0)) + y_paint <= yy && ((int)((int)contourWidth / 2.0)) + x_paint <= xx) {
		setPixelColor(&image[(yy - ((int)((int)contourWidth / 2.0)) + y_paint) * (int)width + (xx - ((int)((int)contourWidth / 2.0)) + x_paint)], pixelcolor);
	      }
	    }
	  }
	} else {
	  setPixelColor(&image[yy * width + xx], pixelcolor);
	}
      }
    }
  }
  
  //updates progress bar
  (*env)->CallVoidMethod(env, thiz, incProg);
  
  //finally free allocated space
  free(pixelcolor);
}