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
rs_allocation image;
rs_script gScript;
int contourWidth = 1;

static int height;
static int width;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {
    
    uchar4 color;
    color.r = 255;
    color.g = 0;
    color.b = 0;
    color.a = 255;
    
    if (rsUnpackColor8888(*v_in).r == 1.0 ) {
    	float minSurrounding = 1.0;
    	for (uint32_t i = 0; i < 3; i++) {
    		for (uint32_t j = 0; j < 3; j++) {
    			//Take care of unsigned types. Values must not be less than zero!
    			if (!(x == 0 && i == 0) &&  !(x == 0 && j == 0) && (x + i) - 1 < width && (y + j) - 1 < height) {
    				minSurrounding = fmin(minSurrounding, (rsUnpackColor8888(rsGetElementAt_uchar4(u, x-1+i, y-1+j))).r);
    			}
    		}
    	}
    	if (minSurrounding == 0.0) {
    		if (contourWidth > 1) {
    			for(int y_paint = 0; y_paint < contourWidth && (y - ((int)(contourWidth / 2.0)) + y_paint) < height-1; y_paint++) {
    				for(int x_paint = 0; x_paint < contourWidth && (x - ((int)(contourWidth / 2.0)) + x_paint) < width-1; x_paint++) {
    					if(((int)(contourWidth / 2.0)) + y_paint <= y && ((int)(contourWidth / 2.0)) + x_paint <= x) {
    						rsSetElementAt_uchar4(image, color, x - ((int)(contourWidth / 2.0)) + x_paint, (y - ((int)(contourWidth / 2.0)) + y_paint));
    					}
    				}
    			}
    		} else {
    			*v_out = color;
    		}
    	}
    }    
}


void filter() {
	width = rsAllocationGetDimX(u);
	height = rsAllocationGetDimY(u);
    rsForEach(gScript, u, image, 0, 0);
}