/*
 * Copyright (C) 2014 Sebastian Soyer
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