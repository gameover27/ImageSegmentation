#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

#include "rs_debug.rsh" 

rs_allocation gIn;
rs_allocation gOut;
rs_script gScript;

float threshold = 0.5;

void root(const float *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    //float4 apixel = rsUnpackColor8888(*v_in);
    //float3 pixel = apixel.rgb;
    
    if (*v_in >= threshold) {
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
    rsForEach(gScript, gIn, gOut, 0, 0);
}