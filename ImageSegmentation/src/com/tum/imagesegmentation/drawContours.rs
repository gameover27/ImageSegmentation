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

void root(const float *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    //float4 apixel = rsUnpackColor8888(*v_in);
    //float3 pixel = apixel.rgb;
    
    uchar4 color;
    color.r = 255;
    color.g = 0;
    color.b = 0;
    color.a = 255;
    
    if (*v_in >= threshold && fmin(fmin(fmin(fmin(fmin(fmin(fmin(fmin(
    		rsGetElementAt_float(u, x, y-1), rsGetElementAt_float(u, x, y)), 
    		rsGetElementAt_float(u, x, y+1)), rsGetElementAt_float(u, x-1, y-1)), 
    		rsGetElementAt_float(u, x+1, y-1)), rsGetElementAt_float(u, x-1, y+1)), 
    		rsGetElementAt_float(u, x+1, y+1)), rsGetElementAt_float(u, x-1, y)), 
    		rsGetElementAt_float(u, x+1, y)) < threshold) {
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