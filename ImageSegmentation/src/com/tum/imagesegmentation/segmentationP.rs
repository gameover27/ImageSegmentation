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

#include "rs_cl.rsh"

rs_allocation imgGrad; //Image gradient
rs_allocation u;
rs_allocation p_x;
rs_allocation p_y;
rs_script gScript;

float tau;
float theta;
float alpha;
float beta;

static int width;
static int height;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 apixel = rsUnpackColor8888(*v_in);
    float3 pixel = apixel.rgb;
    
    float p_tilde_x;
    float p_tilde_y;
    
    float grad_x;
    float grad_y;
    
    float g;
    float norm_term;
    
    if (x >= width-1) {
    	grad_x = 0.0;
    } else {
    	grad_x = rsGetElementAt_float(u, x+1, y) - rsGetElementAt_float(u, x, y);
    }
    if (y >= height-1) {
    	grad_y = 0.0;
    } else {
    	grad_y = rsGetElementAt_float(u, x, y+1) - rsGetElementAt_float(u, x, y);
    }
    
    p_tilde_x = rsGetElementAt_float(p_x, x, y) + (tau / theta) * grad_x;
    p_tilde_y = rsGetElementAt_float(p_y, x, y) + (tau / theta) * grad_y;
    
    g = fmax(1E-37f, exp(pow(fabs(pixel.r), beta) * -1.0f * alpha));
    norm_term = fmax(1.0f, sqrt(pow(p_tilde_x, 2.0) + pow(p_tilde_y, 2.0)) / g);
    
    rsSetElementAt_float(p_x, p_tilde_x / norm_term, x, y);
    rsSetElementAt_float(p_y, p_tilde_y / norm_term, x, y);
    
    //pixel = clamp(pixel,0.0f,1.0f);
    //*v_out = rsPackColorTo8888(pixel.rgb);
    
    //apixel = rsUnpackColor8888(rsGetElementAt_uchar4(u, x, y));
    //factor = rsGetElementAt_float(u, x, y);
    //pixel = apixel.rgb;
    //pixel.r = 1.0;
    //pixel.g = 0.0;
    //pixel.b = 0.0;
    //pixel = clamp(pixel,0.0f,1.0f);
    //rsSetElementAt_uchar4(u, rsPackColorTo8888(pixel.rgb), x, y);
    //*v_out = rsPackColorTo8888(pixel.rgb);
    
}


void filter() {
	width =  rsAllocationGetDimX(imgGrad);
	height =  rsAllocationGetDimY(imgGrad);
    rsForEach(gScript, imgGrad, imgGrad, 0, 0);
}