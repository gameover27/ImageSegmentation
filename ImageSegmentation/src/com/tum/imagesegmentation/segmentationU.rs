#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

#include "rs_debug.rsh"

rs_allocation u;
rs_allocation p_x;
rs_allocation p_y;
rs_script gScript;

float theta;

void root(const float *v_in, float *v_out, const void *usrData, uint32_t x, uint32_t y) {

	float deriv_x = rsGetElementAt_float(p_x, x, y);
	float deriv_y = rsGetElementAt_float(p_y, x, y);
	
	if (x != 0) {
		deriv_x -= rsGetElementAt_float(p_x, x-1, y);
	}
	if (y != 0) {
		deriv_y -= rsGetElementAt_float(p_y, x, y-1);
	}
	
	rsSetElementAt_float(u, rsGetElementAt_float(u, x, y) + (theta * (deriv_x + deriv_y)), x, y);
	
	if (rsGetElementAt_float(u, x, y) > 1.0) {
    	rsSetElementAt_float(u, 1.0, x, y);
    } else if (rsGetElementAt_float(u, x, y) < 0.0) {
    	rsSetElementAt_float(u, 0.0, x, y);
    }

	//rsSetElementAt_float(u, rsGetElementAt_float(u, x, y) + 0.33, x, y);
    
}


void filter() {
    //mImageWidth = rsAllocationGetDimX(u);
    //rsDebug("Image size is ", rsAllocationGetDimX(u), rsAllocationGetDimY(u));
    //rsDebug("Image size is ", mImageWidth);
    rsForEach(gScript, u, u, 0, 0);
}