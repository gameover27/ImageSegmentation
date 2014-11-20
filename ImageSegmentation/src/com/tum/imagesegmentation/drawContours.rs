#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

#include "rs_debug.rsh" 

rs_allocation u;
rs_allocation image;
rs_script gScript;

float threshold = 0.5;
int contourWidth = 1;

static int height;
static int width;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    //float4 apixel = rsUnpackColor8888(*v_in);
    //float3 pixel = apixel.rgb;
    
    uchar4 color;
    color.r = 255;
    color.g = 0;
    color.b = 0;
    color.a = 255;
    
    float4 pixel1 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x, y-1));
    float4 pixel2 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x, y));
    float4 pixel3 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x, y+1));
    float4 pixel4 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x-1, y-1));
    float4 pixel5 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x+1, y-1));
    float4 pixel6 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x-1, y+1));
    float4 pixel7 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x-1, y));
    float4 pixel8 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x+1, y));
    float4 pixel9 = rsUnpackColor8888(rsGetElementAt_uchar4(u, x+1, y+1));
    
    if (rsUnpackColor8888(*v_in).r >= threshold && fmin(fmin(fmin(fmin(fmin(fmin(fmin(fmin(
    		pixel1.r, pixel2.r), 
    		pixel3.r), pixel4.r), 
    		pixel5.r), pixel6.r), 
    		pixel7.r), pixel8.r), 
    		(rsGetElementAt_uchar4(u, x+1, y)).r) < threshold) {
    	if (contourWidth > 1) {
    		for(int y_paint = 0; y_paint < contourWidth && (y - ((int)(contourWidth / 2.0)) + y_paint) < height; y_paint++) {
    			for(int x_paint = 0; x_paint < contourWidth && (x - ((int)(contourWidth / 2.0)) + x_paint) < width; x_paint++) {
    				if(((int)(contourWidth / 2.0)) + y_paint <= y && ((int)(contourWidth / 2.0)) + x_paint <= x) {
    					rsSetElementAt_uchar4(image, color, x - ((int)(contourWidth / 2.0)) + x_paint, (y - ((int)(contourWidth / 2.0)) + y_paint));
    				}
    			}
    		}
    	} else {
    		*v_out = color;
    	}
    } else {
    }
    
}


void filter() {
	width = rsAllocationGetDimX(u);
	height = rsAllocationGetDimY(u);
    rsForEach(gScript, u, image, 0, 0);
}