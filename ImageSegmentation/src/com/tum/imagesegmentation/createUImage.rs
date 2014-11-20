#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

#include "rs_debug.rsh" 

rs_allocation gIn;
rs_allocation gOut;
rs_script gScript;

void root(const float *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {
    uchar value = (uchar)(*v_in * 255);
    v_out->a = 255;
    v_out->r = value;
    v_out->g = value;
    v_out->b = value;
    
}


void filter() {
    rsForEach(gScript, gIn, gOut, 0, 0);
}