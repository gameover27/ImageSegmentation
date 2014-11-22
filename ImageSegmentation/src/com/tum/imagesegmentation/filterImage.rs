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

void root(const uchar4 *v_in, float3 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 pixel;
    float kernelValue;
    
    float3 newValue = 0.f;

    for(int i = (x - kernelWidth/2 > 0 ? 0 : kernelWidth/2 - x); i < kernelWidth && x - (kernelWidth/2) + i < imageWidth; i++) {
    	for (int j = (y - kernelHeight/2 > 0 ? 0 : kernelHeight/2 - y); j < kernelHeight && y - (kernelWidth/2) + j < imageHeight; j++) {
    		kernelValue = rsGetElementAt_float(filterMatrix, i, j);    		
    		pixel = rsUnpackColor8888(rsGetElementAt_uchar4(gIn, x - kernelWidth/2 + i, y - kernelHeight/2 + j));
    		newValue = newValue + kernelValue * pixel.rgb;
    	}
    }
      
    *v_out = newValue;
    
}


void filter() {
	kernelWidth = rsAllocationGetDimX(filterMatrix);
	kernelHeight = rsAllocationGetDimY(filterMatrix);
	imageWidth = rsAllocationGetDimX(gIn);
	imageHeight = rsAllocationGetDimY(gIn);
    rsForEach(gScript, gIn, gOut, 0, 0);
}