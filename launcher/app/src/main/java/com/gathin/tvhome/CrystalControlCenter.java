package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Arrays;

/**
 * Remote-first crystal control center, hidden until the gear button is pressed.
 * The underlying Home screen previews changes without persisting them.
 */
final class CrystalControlCenter {
    interface Host {
        void preview(CrystalAppearance draft);
        void save(CrystalAppearance draft);
        void cancel();
        void chooseWallpaper();
        void restoreWallpaper();
        void chooseTheme();
        void exportTheme(CrystalAppearance draft);
        android.graphics.Bitmap currentWallpaper();
    }

    private final MainActivity activity;
    private final Host host;
    private final CrystalAppearance original;
    private CrystalAppearance draft;
    private Dialog dialog;
    private LinearLayout content, navigation;
    private String selected = "Appearance";
    private boolean completed;
    private final float dp;
    private final int white = Color.WHITE;
    private final int aqua = 0xFF6EF3FF;
    private final int textDim = 0xFFC2E4EF;
    private static final String[] SECTIONS = {
            "Appearance", "Widgets", "Dock Glass", "Glass Material", "Ambient Glow",
            "Magnetic Spotlight", "Movement", "Adaptive Glass", "Wallpaper", "Themes"
    };
    private static final int[] COLORS = {
            0xFFB7F8FF, 0xFF81B7FF, 0xFF84E3B9, 0xFFBAACFF,
            0xFFF7D18F, 0xFFFFACB8, 0xFFFFFFFF, 0xFF143953
    };

    CrystalControlCenter(MainActivity activity, Host host, CrystalAppearance saved) {
        this.activity = activity; this.host = host;
        original = saved.copy(); draft = saved.copy();
        dp = activity.getResources().getDisplayMetrics().density;
    }

    boolean isShowing() { return dialog != null && dialog.isShowing(); }

    void show() {
        if (isShowing()) return;
        completed = false;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout outer = column();
        outer.setPadding(px(16),px(14),px(16),px(12));
        outer.setBackground(panel(0xEB123C57, 27, 0xA1B9F7FF));
        LinearLayout heading = row();
        TextView title = label("◇  Crystal Control Center", 24, white, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, px(48), 1));
        Button close = button("✕", () -> dismiss(false));
        heading.addView(close, new LinearLayout.LayoutParams(px(55), px(45)));
        outer.addView(heading);
        TextView subtitle = label("Customize your Crystal Clear experience", 12, textDim, false);
        outer.addView(subtitle);
        LinearLayout body = row();
        body.setPadding(0,px(10),0,px(8));
        ScrollView left = new ScrollView(activity);
        left.setFillViewport(true);
        navigation = column();
        navigation.setPadding(0, 0, px(10), 0);
        left.addView(navigation);
        body.addView(left, new LinearLayout.LayoutParams(px(213), -1));
        ScrollView right = new ScrollView(activity);
        right.setFillViewport(true);
        content = column();
        content.setPadding(px(18),px(12),px(18),px(12));
        content.setBackground(panel(0xB42E5770, 18, 0x4AB0E7FA));
        right.addView(content);
        body.addView(right, new LinearLayout.LayoutParams(0,-1,1));
        outer.addView(body, new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom = row();
        bottom.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button reset = button("Reset to Crystal Clear", () -> {
            draft = new CrystalAppearance(); change(); rebuild();
        });
        Button cancel = button("Cancel", () -> dismiss(false));
        Button save = button("✓ Save Theme", () -> dismiss(true));
        bottom.addView(reset);
        bottom.addView(cancel);
        bottom.addView(save);
        outer.addView(bottom);

        dialog.setContentView(outer);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = .24f;
            window.setAttributes(attrs);
        }
        dialog.setOnCancelListener(d -> dismiss(false));
        dialog.setOnDismissListener(d -> {
            if (!completed) { completed = true; host.cancel(); }
        });
        dialog.show();
        if (window != null) {
            android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            window.setLayout(Math.min(dm.widthPixels - px(32), px(1100)),
                    Math.min(dm.heightPixels - px(24), px(640)));
            window.setGravity(Gravity.CENTER);
        }
        rebuild();
    }

    void refreshWallpaper() {
        if (isShowing() && "Wallpaper".equals(selected)) rebuild();
    }
    void imported(CrystalAppearance incoming) {
        draft = incoming.copy();
        change();
        rebuild();
    }
    private void dismiss(boolean save) {
        if (completed) return;
        completed = true;
        if (save) host.save(draft.copy());
        else host.cancel();
        if (dialog != null) dialog.dismiss();
    }
    private void change() { host.preview(draft.copy()); }

    private void rebuild() {
        if (content == null) return;
        navigation.removeAllViews();
        for (String tab : SECTIONS) {
            Button b = button(tab, () -> { selected = tab; rebuild(); });
            if (selected.equals(tab)) b.setBackground(panel(0xA12EC0D6, 12, 0xFFE3FFFF));
            navigation.addView(b, new LinearLayout.LayoutParams(-1,px(48)));
        }
        content.removeAllViews();
        addTitle(selected);
        switch(selected) {
            case "Appearance": appearance(); break;
            case "Widgets": widgets(); break;
            case "Dock Glass": dock(); break;
            case "Glass Material": glassMaterial(); break;
            case "Ambient Glow": ambientGlow(); break;
            case "Magnetic Spotlight": spotlight(); break;
            case "Movement": movement(); break;
            case "Adaptive Glass": adaptive(); break;
            case "Wallpaper": wallpaper(); break;
            case "Themes": themes(); break;
        }
    }

    private void appearance() {
        description("Live previews affect only this preview launcher until Save.");
        content.addView(label("Crystal Clear · Alpine", 17, aqua, true));
        description("Wallpaper, widgets and five dock tiles remain in their fixed positions.");
        content.addView(button("Preview Current Home for 2 seconds", () -> {
            host.preview(draft.copy());
            if (dialog == null) return;
            dialog.hide();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!completed && dialog != null) dialog.show();
            }, 2000L);
        }));
        content.addView(button("Locked Option B · Reset Movement", () -> {
            draft.motion="magnetic"; draft.duration=470; draft.easing="smooth";
            change(); rebuild();
        }));
        content.addView(label("Quick appearance presets", 15, white, true));
        content.addView(button("Crystal Clear",()->preset("crystal")));
        content.addView(button("Emerald",()->preset("emerald")));
        content.addView(button("Sapphire",()->preset("sapphire")));
        content.addView(button("Sunset",()->preset("sunset")));
    }

    private void widgets() {
        description("Customize the two live Prayer Times / Weather cards.");
        colors("Widget glass tint", draft.widgetTint, c -> draft.widgetTint = c);
        slider("Widget Glass Opacity", draft.widgetOpacity, 0,100, x->draft.widgetOpacity=x);
        colors("Widget Text Color", draft.widgetText, c -> draft.widgetText=c);
        colors("Prayer Highlight Color", draft.widgetAccent, c -> draft.widgetAccent=c);
        slider("Widget Highlight Intensity", draft.widgetHighlight,0,100,x->draft.widgetHighlight=x);
    }
    private void dock() {
        description("Adjust glass material without moving the five app tiles.");
        colors("Dock Glass Tint", draft.dockTint, c->draft.dockTint=c);
        slider("Dock Glass Opacity", draft.dockOpacity,0,100,x->draft.dockOpacity=x);
        slider("Border Opacity",draft.dockBorder,0,100,x->draft.dockBorder=x);
        slider("Reflection Intensity",draft.dockReflection,0,100,x->draft.dockReflection=x);
    }
    private void glassMaterial() {
        description("Cached wallpaper glass only; no per-frame blur. 0% refraction adds no white highlight.");
        choices("Frosted Blur", new String[]{"off","low","medium"},
                new String[]{"Off · Clear","Low · Original","Medium · Frosted"},
                draft.glassBlur, v->draft.glassBlur=v);
        slider("Glass Clarity",draft.glassClarity,0,100,x->draft.glassClarity=x);
        CheckBox grain = new CheckBox(activity);
        grain.setText("Subtle glass grain (static)");
        grain.setTextColor(white); grain.setTextSize(16);
        grain.setChecked(draft.glassGrain);
        grain.setOnCheckedChangeListener((b,checked)->{ draft.glassGrain=checked; change(); });
        content.addView(grain);
        slider("Refraction Strength",draft.glassRefraction,0,100,x->draft.glassRefraction=x);
        slider("Corner Radius (dp)",draft.glassRadius,12,28,x->draft.glassRadius=x);
    }
    private void ambientGlow() {
        description("Ambient glow stays behind the dock and is independent of locked Spotlight B.");
        CheckBox enabled = new CheckBox(activity);
        enabled.setText("Enable Ambient Glow");
        enabled.setTextColor(white); enabled.setTextSize(16);
        enabled.setChecked(draft.ambientEnabled);
        enabled.setOnCheckedChangeListener((b,checked)->{draft.ambientEnabled=checked;change();});
        content.addView(enabled);
        colors("Ambient Glow Color",draft.ambientColor,c->draft.ambientColor=c);
        slider("Ambient Glow Intensity",draft.ambientIntensity,0,100,x->draft.ambientIntensity=x);
        slider("Ambient Glow Spread",draft.ambientSpread,0,100,x->draft.ambientSpread=x);
        choices("Glow Mode",new String[]{"static","breathing"},
                new String[]{"Static · Best Performance","Breathing · Subtle Pulse"},
                draft.ambientMode,v->draft.ambientMode=v);
        choices("Breathing Speed",new String[]{"0","1"},
                new String[]{"Slow","Normal"},Integer.toString(draft.ambientBreathingSpeed),
                v->draft.ambientBreathingSpeed=Integer.parseInt(v));
    }
    private void spotlight() {
        description("Wide diffuse light above the glass tray, behind the tile labels.");
        colors("Spotlight Color", draft.spotlightTint, c->draft.spotlightTint=c);
        slider("Glow Intensity", draft.spotlightIntensity,0,100,x->draft.spotlightIntensity=x);
        slider("Softness", draft.spotlightSoftness,0,100,x->draft.spotlightSoftness=x);
    }
    private void movement() {
        description("Locked Option B is always restorable. The preview affects focus only.");
        choices("Movement Preset",new String[]{"magnetic","bloom","satin","instant"},
                new String[]{"Magnetic Spotlight B","Soft Bloom","Satin Refraction","Instant Focus"},
                draft.motion,v->draft.motion=v);
        slider("Transition Duration (ms)",draft.duration,150,800,x->draft.duration=x);
        choices("Easing",new String[]{"smooth","soft","linear"},
                new String[]{"Smooth","Soft","Linear"},draft.easing,v->draft.easing=v);
        content.addView(button("Reset to Locked Option B · 470 ms",()->{
            draft.motion="magnetic";draft.duration=470;draft.easing="smooth";
            change();rebuild();
        }));
    }
    private void adaptive() {
        description("Analyze the new wallpaper once, then adapt glass tint without frame-by-frame sampling.");
        CheckBox toggle = new CheckBox(activity);
        toggle.setText("Adaptive Glass");
        toggle.setTextColor(white);
        toggle.setChecked(draft.adaptive);
        toggle.setTextSize(16);
        toggle.setOnCheckedChangeListener((button,checked)->{draft.adaptive=checked;change();});
        content.addView(toggle);
        slider("Adapt Sensitivity",draft.adaptSensitivity,0,100,x->draft.adaptSensitivity=x);
        choices("Glass Style", new String[]{"soft","balanced","strong"},
                new String[]{"Soft","Balanced","Strong"},draft.adaptStyle,v->draft.adaptStyle=v);
    }
    private void wallpaper() {
        description("Choose an image from TV storage or USB. Saved wallpaper is backed up first.");
        android.graphics.Bitmap image = host.currentWallpaper();
        if (image != null && !image.isRecycled()) {
            ImageView view = new ImageView(activity);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setImageBitmap(image);
            content.addView(view,new LinearLayout.LayoutParams(-1,px(152)));
        }
        content.addView(button("Choose Image (TV / USB)",host::chooseWallpaper));
        choices("Fit / Crop", new String[]{"crop","fit"},new String[]{"Full-screen Crop","Fit Entire Image"},
                draft.wallpaperFit,v->draft.wallpaperFit=v);
        slider("Wallpaper Brightness",draft.wallpaperBrightness,35,150,x->draft.wallpaperBrightness=x);
        slider("Wallpaper Saturation",draft.wallpaperSaturation,0,180,x->draft.wallpaperSaturation=x);
        slider("Wallpaper Blur",draft.wallpaperBlur,0,20,x->draft.wallpaperBlur=x);
        content.addView(button("Restore Original Alpine Wallpaper",()->{
            host.restoreWallpaper();draft.wallpaperFit="crop";change();rebuild();
        }));
    }
    private void themes() {
        description("Preview a palette, save it, or export a JSON backup to your TV.");
        content.addView(button("Crystal Clear · Locked Default",()->{
            draft=CrystalAppearance.preset("crystal");change();rebuild();
        }));
        content.addView(button("Emerald",()->preset("emerald")));
        content.addView(button("Sapphire",()->preset("sapphire")));
        content.addView(button("Sunset",()->preset("sunset")));
        content.addView(button("Export Theme Backup · JSON",()->{
            host.exportTheme(draft.copy());
        }));
        content.addView(button("Import Theme from TV / USB",host::chooseTheme));
    }
    private void preset(String id) { draft=CrystalAppearance.preset(id);change();rebuild(); }

    private interface ColorSet { void set(int value); }
    private void colors(String name, int value, ColorSet setter) {
        content.addView(label(name,14,white,true));
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER_VERTICAL);
        for (int color : COLORS) {
            Button swatch=new Button(activity);
            swatch.setContentDescription(name+" "+String.format("#%06X",color & 0xFFFFFF));
            swatch.setText(value==color?"✓":" ");
            swatch.setTextColor(color == Color.WHITE ? Color.BLACK:Color.WHITE);
            swatch.setTextSize(10f);
            swatch.setBackground(panel(color,16,value==color?Color.WHITE:0x44FFFFFF));
            swatch.setOnClickListener(v->{setter.set(color);change();rebuild();});
            r.addView(swatch,new LinearLayout.LayoutParams(0,px(39),1));
        }
        content.addView(r);
    }

    private interface ValueSet { void set(int x); }
    private void slider(String title,int value,int min,int max,ValueSet setter){
        LinearLayout header=row();
        header.addView(label(title,14,white,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView number=label(Integer.toString(value)+(max==100?"%":""),13,aqua,true);
        header.addView(number);
        content.addView(header);
        SeekBar bar=new SeekBar(activity);
        bar.setMax(max-min);
        bar.setProgress(value-min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar sb){}
            public void onStopTrackingTouch(SeekBar sb){}
            public void onProgressChanged(SeekBar sb,int progress,boolean fromUser) {
                if (fromUser) {
                    int next=min+progress;
                    number.setText(Integer.toString(next)+(max==100?"%":""));
                    setter.set(next);change();
                }
            }
        });
        content.addView(bar,new LinearLayout.LayoutParams(-1,px(42)));
    }

    private interface StringSet {void set(String value);}
    private void choices(String title,String[] ids,String[] labels,String current,StringSet setter) {
        content.addView(label(title,14,white,true));
        for(int i=0;i<ids.length;i++){
            final String id=ids[i];
            String prefix=current.equals(id)?"◉  ":"◯  ";
            Button b=button(prefix+labels[i],()->{setter.set(id);change();rebuild();});
            if(current.equals(id)) b.setBackground(panel(0x9247A5BE,12,0xFF79F5FF));
            content.addView(b);
        }
    }

    private void addTitle(String text) {
        content.addView(label(text,21,white,true));
    }
    private void description(String text){
        TextView hint=label(text,13,textDim,false);
        hint.setPadding(0,px(5),0,px(13));
        content.addView(hint);
    }
    private Button button(String text, Runnable action) {
        Button b=new Button(activity);
        b.setAllCaps(false);b.setText(text);b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackground(panel(0x7A2B5F78,12,0x55AEEFFF));
        b.setFocusable(true);
        b.setPadding(px(9),px(2),px(9),px(2));
        b.setOnClickListener(v->action.run());
        b.setOnFocusChangeListener((v,has)-> {
            if (has) b.setBackground(panel(0xB927BED6,12,0xFFFFFFFF));
            else b.setBackground(panel(0x7A2B5F78,12,0x55AEEFFF));
        });
        return b;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(activity);t.setText(s);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMinHeight(px(28));
        return t;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private int px(float x){return Math.round(x*dp);}
    private GradientDrawable panel(int color,int radius,int stroke){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{color,color});
        d.setCornerRadius(px(radius));d.setStroke(px(1),stroke);
        return d;
    }
}
