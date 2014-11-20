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
rs_script gScript;

int redValue = 255;
int greenValue = 255;
int blueValue = 255;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    //float4 apixel = rsUnpackColor8888(*v_in);
    //float3 pixel = apixel.rgb;
    
    if (v_in->r == redValue && v_in->g == greenValue && v_in->b == blueValue) {
    	v_out->a = 255;
    	v_out->r = 42;
    	v_out->g = 42;
    	v_out->b = 42;
    }
    
    //pixel = 0.5;
    //pixel = clamp(pixel,0.0f,1.0f);
    //*v_out = rsPackColorTo8888(pixel.rgb);
    
    //apixel = rsUnpackColor8888(rsGetElementAt_uchar4(u, x, y));
    //float factor = rsGetElementAt_float(u, x, y);
    //pixel = apixel.rgb;
    //pixel.r = 1.0;
    //pixel.g = 0.0;
    //pixel.b = 0.0;
    //pixel = clamp(pixel,0.0f,1.0f);
    //rsSetElementAt_uchar4(u, rsPackColorTo8888(pixel.rgb), x, y);
    //*v_out = rsPackColorTo8888(pixel.rgb);
    
}


void filter() {
    rsForEach(gScript, gIn, gOut, 0, 0);
}