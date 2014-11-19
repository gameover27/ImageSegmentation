#pragma version(1)
#pragma rs java_package_name(com.tum.imagesegmentation)

float brightnessValue;

rs_allocation gIn; //Image gradient
rs_allocation gOut; //p_x
rs_allocation p_y;
rs_script gScript;

static int mImageWidth;
const uchar4 *gPixels;

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {

    float4 apixel = rsUnpackColor8888(*v_in);
    float3 pixel = apixel.rgb;
    float factor = brightnessValue;
    pixel = 0.5;
    pixel = clamp(pixel,0.0f,1.0f);
    *v_out = rsPackColorTo8888(pixel.rgb);
    
    //apixel = rsUnpackColor8888(rsGetElementAt_uchar4(u, x, y));
    //factor = rsGetElementAt_float(u, x, y);
    //pixel = apixel.rgb;
    pixel.r = 1.0;
    pixel.g = 0.0;
    pixel.b = 0.0;
    pixel = clamp(pixel,0.0f,1.0f);
    //rsSetElementAt_uchar4(u, rsPackColorTo8888(pixel.rgb), x, y);
    *v_out = rsPackColorTo8888(pixel.rgb);
    
}


void filter() {
    mImageWidth = rsAllocationGetDimX(gIn);
    rsDebug("Image size is ", rsAllocationGetDimX(gIn), rsAllocationGetDimY(gOut));
    rsForEach(gScript, gIn, gOut, 0, 0);
}