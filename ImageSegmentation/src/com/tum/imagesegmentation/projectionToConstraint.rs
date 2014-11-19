#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

#include "rs_debug.rsh" 

float brightnessValue;

rs_allocation gIn;
rs_allocation gOut;
rs_script gScript;

int foreground = 0;
int initialized = 0;

void root(const uchar4 *v_in, float *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 apixel = rsUnpackColor8888(*v_in);
    float3 pixel = apixel.rgb;
    
    if (pixel.r != 0.0) {
    	//rsSetElementAt_float(u, 0.5, x, y);
    	if (foreground > 0) {
    		*v_out = 1.0;
    	} else {
    		*v_out = 0.0;
    	}
    } else if (initialized == 0) {
    	*v_out = 0.5;
    }
    
}


void filter() {
    rsForEach(gScript, gIn, gOut, 0, 0);
}