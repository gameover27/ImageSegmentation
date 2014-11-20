#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

#include "rs_debug.rsh" 

rs_allocation gIn;
rs_script gScript;

float threshold = 0.5;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 pixel = rsUnpackColor8888(*v_in);
    
    if (pixel.r >= threshold) {
    	v_out->a = 255;
    	v_out->r = 255;
    	v_out->g = 255;
    	v_out->b = 255;
    } else {
    	v_out->a = 255;
    	v_out->r = 0;
    	v_out->g = 0;
    	v_out->b = 0;
    }
    
}


void filter() {
    rsForEach(gScript, gIn, gIn, 0, 0);
}