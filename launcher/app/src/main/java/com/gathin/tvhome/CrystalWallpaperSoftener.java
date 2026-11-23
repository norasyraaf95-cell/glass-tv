package com.gathin.tvhome;

import android.graphics.Bitmap;

/** One-shot wallpaper softness prepared off the UI thread after slider settles. */
final class CrystalWallpaperSoftener {
    private CrystalWallpaperSoftener(){}

    static Bitmap prepare(Bitmap source,int strength) {
        if (source == null || source.isRecycled() || strength <= 0) return source;
        int w=Math.max(2,Math.min(source.getWidth(),512));
        int h=Math.max(2,Math.round(source.getHeight()*w/(float)source.getWidth()));
        Bitmap small=Bitmap.createScaledBitmap(source,w,h,true);
        int[] pix=new int[w*h], tmp=new int[w*h], out=new int[w*h];
        small.getPixels(pix,0,w,0,0,w,h);
        int radius=Math.max(1,Math.min(32,strength*2));
        // Linear-time separable box blur: two passes, fixed memory, no GPU synchronisation.
        for(int y=0;y<h;y++) {
            int base=y*w;
            for(int x=0;x<w;x++){
                int r=0,g=0,b=0,count=0;
                for(int k=Math.max(0,x-radius);k<=Math.min(w-1,x+radius);k++){
                    int c=pix[base+k];
                    r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;count++;
                }
                tmp[base+x]=0xFF000000|((r/count)<<16)|((g/count)<<8)|(b/count);
            }
        }
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int r=0,g=0,b=0,count=0;
                for(int k=Math.max(0,y-radius);k<=Math.min(h-1,y+radius);k++){
                    int c=tmp[k*w+x];
                    r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;count++;
                }
                out[y*w+x]=0xFF000000|((r/count)<<16)|((g/count)<<8)|(b/count);
            }
        }
        Bitmap result=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        result.setPixels(out,0,w,0,0,w,h);
        if(small!=source) small.recycle();
        return result;
    }
}
