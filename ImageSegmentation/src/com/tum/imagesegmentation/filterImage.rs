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

rs_allocation gIn;
rs_allocation gOut;
rs_allocation filterMatrix;
rs_script gScript;

static int kernelWidth;
static int kernelHeight;
static int imageWidth;
static int imageHeight;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 pixel;
    float kernelValue;
    
    float3 newValue = 0.f;

    for(int i = 0; i < kernelWidth; i++) {
    	for (int j = 0; j < kernelHeight; j++) {
    		kernelValue = rsGetElementAt_float(filterMatrix, i, j);
    		
    		if (x >= kernelWidth/2 + i && y >= kernelHeight/2 + j && x <= imageWidth - 1 + kernelWidth/2 - i && y <= imageHeight - 1 + kernelHeight/2 - j ) {
    			pixel = rsUnpackColor8888(rsGetElementAt_uchar4(gIn, x - kernelWidth/2 + i, y - kernelHeight/2 + j));
    			newValue.r = newValue.r + kernelValue * pixel.r;
    			newValue.g = newValue.g + kernelValue * pixel.g;
    			newValue.b = newValue.b + kernelValue * pixel.b;
    		}
    	}
    }    
    newValue.r = fabs(newValue.r);
    newValue.g = fabs(newValue.g);
    newValue.b = fabs(newValue.b);
    
    newValue = clamp(newValue, 0.0f, 1.0f);
    
    *v_out = rsPackColorTo8888(newValue);
    
}


void filter() {
	kernelWidth = rsAllocationGetDimX(filterMatrix);
	kernelHeight = rsAllocationGetDimY(filterMatrix);
	imageWidth = rsAllocationGetDimX(gIn);
	imageHeight = rsAllocationGetDimY(gIn);
    rsForEach(gScript, gIn, gOut, 0, 0);
}