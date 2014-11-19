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