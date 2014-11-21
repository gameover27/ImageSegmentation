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

void root(const float3 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float3 pixelX = *v_in;
    float3 pixelY = rsGetElementAt_float3(gY,x,y);
    
    float combinedValue = clamp(sqrt(pixelX.r * pixelX.r + pixelY.r * pixelY.r), 0.f, 1.f);
    
    
    *v_out = rsPackColorTo8888(combinedValue, combinedValue, combinedValue);
    
}


void filter() {
    rsForEach(gScript, gX, gOut, 0, 0);
}