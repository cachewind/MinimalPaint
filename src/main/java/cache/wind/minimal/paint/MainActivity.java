/*
 * Copyright (C) 2014 Valerio Bozzolan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cache.wind.minimal.paint;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

@SuppressLint("ClickableViewAccessibility")
public class MainActivity extends GraphicsActivity implements
        ColorPickerDialog.OnColorChangedListener {

    public static final int DEFAULT_BRUSH_SIZE = 10;

    private MinimalPaintView mMinimalPaintView;
    private Paint mPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // it removes the title from the actionbar(more space for icons?)
        // this.getActionBar().setDisplayShowTitleEnabled(false);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(DEFAULT_BRUSH_SIZE);

        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 6, 3.5f);

        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);

        mMinimalPaintView = new MinimalPaintView(this);
        mMinimalPaintView.setPaint(mPaint);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.BELOW, R.id.toolbar);
        ((ViewGroup) findViewById(R.id.root)).addView(mMinimalPaintView, params);

        updateMode();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT)
                .show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 3000);
    }

    public void colorChanged(int color) {
        mPaint.setColor(color);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.undo_menu).setVisible(mMinimalPaintView.hasUndo());
        menu.findItem(R.id.redo_menu).setVisible(mMinimalPaintView.hasRedo());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xFF);

        switch (item.getItemId()) {
            case R.id.normal_brush_menu:
                mPaint.setMaskFilter(null);
                break;
            case R.id.color_menu:
                new ColorPickerDialog(this, this, mPaint.getColor()).show();
                break;
            case R.id.emboss_menu:
                mPaint.setMaskFilter(mEmboss);
                break;
            case R.id.blur_menu:
                mPaint.setMaskFilter(mBlur);
                break;
            case R.id.size_menu:
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.brush_size_layout,
                        (ViewGroup) findViewById(R.id.root), false);
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setView(layout);
                builder.setTitle(R.string.brush_size);
                builder.setCancelable(true);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setCancelable(true);
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                SeekBar sb = (SeekBar) layout.findViewById(R.id.brushSizeSeekBar);
                sb.setProgress(getStrokeSize());
                final TextView txt = (TextView) layout
                        .findViewById(R.id.sizeValueTextView);
                txt.setText(String.format(
                        getResources().getString(R.string.your_selected_size_is),
                        getStrokeSize() + 1));
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  final int progress, boolean fromUser) {
                        // Do something here with new value
                        mPaint.setStrokeWidth(progress);
                        txt.setText(String.format(
                                getResources().getString(
                                        R.string.your_selected_size_is), progress + 1));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                break;
            case R.id.erase_menu:
                LayoutInflater inflater_e = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout_e = inflater_e.inflate(R.layout.brush_size_layout,
                        (ViewGroup) findViewById(R.id.root), false);
                AlertDialog.Builder builder_e = new AlertDialog.Builder(this)
                        .setView(layout_e);
                builder_e.setTitle(R.string.erase_size);
                final AlertDialog alertDialog_e = builder_e.create();
                alertDialog_e.setCancelable(true);
                alertDialog_e.setCanceledOnTouchOutside(true);
                alertDialog_e.show();
                SeekBar sb_e = (SeekBar) layout_e.findViewById(R.id.brushSizeSeekBar);
                sb_e.setProgress(getStrokeSize());
                final TextView txt_e = (TextView) layout_e
                        .findViewById(R.id.sizeValueTextView);
                txt_e.setText(String.format(
                        getResources().getString(R.string.your_selected_size_is),
                        getStrokeSize() + 1));
                sb_e.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  final int progress, boolean fromUser) {
                        // Do something here with new value
                        mPaint.setStrokeWidth(progress);
                        txt_e.setText(String.format(
                                getResources().getString(
                                        R.string.your_selected_size_is), progress + 1));
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                // mPaint.setColor(bgColor);
                mPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
                break;
            case R.id.clear_all_menu:
                mMinimalPaintView.reset();
                break;
            case R.id.undo_menu:
                mMinimalPaintView.undo();
                break;
            case R.id.redo_menu:
                mMinimalPaintView.redo();
                break;
            case R.id.save_menu:
                takeScreenshot(true);
                break;
            case R.id.share_menu:
                File screenshotPath = takeScreenshot(false);
                Intent i = new Intent();
                i.setAction(Intent.ACTION_SEND);
                i.setType("image/png");
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.share_title_template));
                i.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.share_text_template));
                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenshotPath));
                try {
                    startActivity(Intent.createChooser(i,
                            getString(R.string.toolbox_share_title)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this.getApplicationContext(),
                            R.string.no_way_to_share,
                            Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.about_menu:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        updateMode();
        return super.onOptionsItemSelected(item);
    }

    /**
     * This takes the screenshot of the whole screen. Is this a good thing?
     */
    private File takeScreenshot(boolean showToast) {
        View v = findViewById(R.id.CanvasId);
        v.setDrawingCacheEnabled(true);
        Bitmap cachedBitmap = v.getDrawingCache();
        Bitmap copyBitmap = cachedBitmap.copy(Bitmap.Config.RGB_565, true);
        v.destroyDrawingCache();
        FileOutputStream output = null;
        File file = null;
        try {
            File path = Places.getScreenshotFolder();
            Calendar cal = Calendar.getInstance();

            file = new File(path,

                    cal.get(Calendar.YEAR) + "_" + (1 + cal.get(Calendar.MONTH)) + "_"
                            + cal.get(Calendar.DAY_OF_MONTH) + "_"
                            + cal.get(Calendar.HOUR_OF_DAY) + "_"
                            + cal.get(Calendar.MINUTE) + "_" + cal.get(Calendar.SECOND)
                            + ".png");
            output = new FileOutputStream(file);
            copyBitmap.compress(CompressFormat.PNG, 100, output);
        } catch (FileNotFoundException e) {
            file = null;
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        if (file != null) {
            if (showToast)
                Toast.makeText(
                        getApplicationContext(),
                        String.format(
                                getResources().getString(
                                        R.string.saved_your_location_to),
                                file.getAbsolutePath()), Toast.LENGTH_LONG)
                        .show();
            // sending a broadcast to the media scanner so it will scan the new
            // screenshot.
            Intent requestScan = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            requestScan.setData(Uri.fromFile(file));
            sendBroadcast(requestScan);

            return file;
        } else {
            return null;
        }
    }

    private int getStrokeSize() {
        return (int) mPaint.getStrokeWidth();
    }

    private void updateMode() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (mPaint.getXfermode() == null) {
                MaskFilter filter = mPaint.getMaskFilter();
                if (filter == mEmboss) {
                    actionBar.setSubtitle(getString(R.string.mode_format, getString(R.string.emboss)));
                } else if (filter == mBlur) {
                    actionBar.setSubtitle(getString(R.string.mode_format, getString(R.string.blur)));
                } else {
                    actionBar.setSubtitle(getString(R.string.mode_format, getString(R.string.normal)));
                }
            } else {
                actionBar.setSubtitle(getString(R.string.mode_format, getString(R.string.erase)));
            }
        }
    }
}