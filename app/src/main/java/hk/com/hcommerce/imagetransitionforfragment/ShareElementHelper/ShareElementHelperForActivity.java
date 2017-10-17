package hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


import java.io.File;

import hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.animations.EnterScreenAnimations;
import hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.animations.ExitScreenAnimations;

import static hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.StaticStringAndInt.IMAGE_FILE_KEY;
import static hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.StaticStringAndInt.KEY_SCALE_TYPE;
import static hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.StaticStringAndInt.KEY_THUMBNAIL_INIT_HEIGHT;
import static hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.StaticStringAndInt.KEY_THUMBNAIL_INIT_LEFT_POSITION;
import static hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.StaticStringAndInt.KEY_THUMBNAIL_INIT_TOP_POSITION;
import static hk.com.hcommerce.imagetransitionforfragment.ShareElementHelper.StaticStringAndInt.KEY_THUMBNAIL_INIT_WIDTH;


public class ShareElementHelperForActivity {

    private final Picasso mImageDownloader;
    private EnterScreenAnimations mEnterScreenAnimations;
    private ExitScreenAnimations mExitScreenAnimations;

    public ShareElementHelperForActivity(Context context) {
        mImageDownloader = Picasso.with(context);
    }

    private ImageView mTransitionImage;

    public void startActivityForShareElement(Activity fromActivity, Class<?> toActivityClass, File imageFile, final ImageView formImage) {
        int[] screenLocation = new int[2];
        formImage.getLocationInWindow(screenLocation);
        Intent intent = getStartIntent(fromActivity, toActivityClass, imageFile, screenLocation[0],
                screenLocation[1],
                formImage.getWidth(),
                formImage.getHeight(),
                formImage.getScaleType());

        fromActivity.startActivity(intent);


    }


    private static Intent getStartIntent(Activity formActivity, Class<?> toActivityClass, File imageFile, int left, int top, int width, int height, ImageView.ScaleType scaleType) {

        Intent startIntent = new Intent(formActivity, toActivityClass);
        startIntent.putExtra(IMAGE_FILE_KEY, imageFile);

        startIntent.putExtra(KEY_THUMBNAIL_INIT_TOP_POSITION, top);
        startIntent.putExtra(KEY_THUMBNAIL_INIT_LEFT_POSITION, left);
        startIntent.putExtra(KEY_THUMBNAIL_INIT_WIDTH, width);
        startIntent.putExtra(KEY_THUMBNAIL_INIT_HEIGHT, height);
        startIntent.putExtra(KEY_SCALE_TYPE, scaleType);

        return startIntent;
    }


    public void startEnterAnimation(Activity activity, ImageView toImageView, View mainContainer, Bundle savedInstanceState) {
        initializeTransitionView(activity);
        mEnterScreenAnimations = new EnterScreenAnimations(mTransitionImage, toImageView, mainContainer);
        mExitScreenAnimations = new ExitScreenAnimations(mTransitionImage, toImageView, mainContainer);
        File imageFile = (File) activity.getIntent().getSerializableExtra(IMAGE_FILE_KEY);
        initializeEnlargedImageAndRunAnimation(savedInstanceState, imageFile, toImageView);

    }

    public void startExitAnimation(Activity activity) {
        mEnterScreenAnimations.cancelRunningAnimations();

//        Log.v(TAG, "onBackPressed, mExitingAnimation " + mExitingAnimation);

        Bundle initialBundle = activity.getIntent().getExtras();
        int toTop = initialBundle.getInt(KEY_THUMBNAIL_INIT_TOP_POSITION);
        int toLeft = initialBundle.getInt(KEY_THUMBNAIL_INIT_LEFT_POSITION);
        int toWidth = initialBundle.getInt(KEY_THUMBNAIL_INIT_WIDTH);
        int toHeight = initialBundle.getInt(KEY_THUMBNAIL_INIT_HEIGHT);

        mExitScreenAnimations.playExitAnimations(
                toTop,
                toLeft,
                toWidth,
                toHeight,
                mEnterScreenAnimations.getInitialThumbnailMatrixValues());
    }


    /**
     * This method waits for the main "big" image is loaded.
     * And then if activity is started for the first time - it runs "entering animation"
     * <p>
     * Activity is entered fro the first time if saveInstanceState is null
     */

    private void initializeEnlargedImageAndRunAnimation(final Bundle savedInstanceState, File imageFile, final ImageView toImageView) {
//        Log.v(TAG, "initializeEnlargedImageAndRunAnimation");

        mImageDownloader.load(imageFile).into(toImageView, new Callback() {

            /**
             * Image is loaded when this method is called
             */
            @Override
            public void onSuccess() {
//                Log.v(TAG, "onSuccess, mEnlargedImage");

                // In this callback we already have image set into ImageView and we can use it's Matrix for animation
                // But we have to wait for final measurements. We use OnPreDrawListener to be sure everything is measured

                if (savedInstanceState == null) {
                    // if savedInstanceState is null activity is started for the first time.
                    // run the animation
                    runEnteringAnimation(toImageView);
                } else {
                    // activity was retrieved from recent apps. No animation needed, just load the image
                }
            }

            @Override
            public void onError() {
                // CAUTION: on error is not handled. If OutOfMemory emerged during image loading we have to handle it here
//                Log.v(TAG, "onError, mEnlargedImage");
            }
        });
    }


    /**
     * This method does very tricky part:
     * <p>
     * It sets up {@link android.view.ViewTreeObserver.OnPreDrawListener}
     * When onPreDraw() method is called the layout is already measured.
     * It means that we can use locations of images on the screen at tis point.
     * <p>
     * 1. When first frame is rendered we start animation.
     * 2. We just let second frame to render
     * 3. Make a view on the previous screen invisible and remove onPreDrawListener
     * <p>
     * Why do we do that:
     * The Android rendering system is double-buffered.
     * Similar technique is used in the SDK. See here : {@link android.app.EnterTransitionCoordinator#startSharedElementTransition}
     * <p>
     * You can read more about it here : https://source.android.com/devices/graphics/architecture.html
     */
    private void runEnteringAnimation(final ImageView toImageView) {
//        Log.v(TAG, "runEnteringAnimation, addOnPreDrawListener");

        toImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            int mFrames = 0;

            @Override
            public boolean onPreDraw() {
                // When this method is called we already have everything laid out and measured so we can start our animation
//                Log.v(TAG, "onPreDraw, mFrames " + mFrames);

                switch (mFrames++) {
                    case 0:
                        /**
                         * 1. start animation on first frame
                         */
                        final int[] finalLocationOnTheScreen = new int[2];
                        toImageView.getLocationOnScreen(finalLocationOnTheScreen);

                        mEnterScreenAnimations.playEnteringAnimation(
                                finalLocationOnTheScreen[0], // left
                                finalLocationOnTheScreen[1], // top
                                toImageView.getWidth(),
                                toImageView.getHeight());

                        return true;
                    case 1:
                        /**
                         * 2. Do nothing. We just draw this frame
                         */

                        return true;
                }
                /**
                 * 3.
                 * Make view on previous screen invisible on after this drawing frame
                 * Here we ensure that animated view will be visible when we make the viw behind invisible
                 */
//                Log.v(TAG, "run, onAnimationStart");
                // TODO: 17/10/2017  ???
//                mBus.post(new ChangeImageThumbnailVisibility(false));

                toImageView.getViewTreeObserver().removeOnPreDrawListener(this);

//                Log.v(TAG, "onPreDraw, << mFrames " + mFrames);

                return true;
            }
        });
    }

    private void initializeTransitionView(Activity activity) {

        FrameLayout androidContent = (FrameLayout) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        mTransitionImage = new ImageView(activity);
        androidContent.addView(mTransitionImage);

        Bundle bundle = activity.getIntent().getExtras();

        int thumbnailTop = bundle.getInt(KEY_THUMBNAIL_INIT_TOP_POSITION)
                - getStatusBarHeight(activity);
        int thumbnailLeft = bundle.getInt(KEY_THUMBNAIL_INIT_LEFT_POSITION);
        int thumbnailWidth = bundle.getInt(KEY_THUMBNAIL_INIT_WIDTH);

        int thumbnailHeight = bundle.getInt(KEY_THUMBNAIL_INIT_HEIGHT);

        ImageView.ScaleType scaleType = (ImageView.ScaleType) bundle.getSerializable(KEY_SCALE_TYPE);


        // We set initial margins to the view so that it was situated at exact same spot that view from the previous screen were.
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mTransitionImage.getLayoutParams();
        layoutParams.height = thumbnailHeight;
        layoutParams.width = thumbnailWidth;
        layoutParams.setMargins(thumbnailLeft, thumbnailTop, 0, 0);

        File imageFile = (File) activity.getIntent().getSerializableExtra(IMAGE_FILE_KEY);
        mTransitionImage.setScaleType(scaleType);

        mImageDownloader.load(imageFile).noFade().into(mTransitionImage);
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
