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

rs_allocation gX;
rs_allocation gY;
rs_allocation gOut;

rs_script gScript;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 pixelX = rsUnpackColor8888(*v_in);
    float4 pixelY = rsUnpackColor8888(rsGetElementAt_uchar4(gY,x,y));
    
    float3 result = sqrt(dot(pixelX.rgb, pixelX.rgb) + dot(pixelY.rgb, pixelY.rgb)) ;
    
    
    *v_out = rsPackColorTo8888(clamp(result, 0.0f, 1.0f));
    
    
    //newValue = clamp(newValue, 0.0f, 1.0f);
    
    //*v_out = rsPackColorTo8888(newValue);
    
}


void filter() {
    rsForEach(gScript, gX, gOut, 0, 0);
}