/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso;

import static com.squareup.picasso.BitmapHunter.forRequest;
import static com.squareup.picasso.Picasso.LoadedFrom.MEMORY;
import static com.squareup.picasso.Utils.checkNotMain;
import static com.squareup.picasso.Utils.createKey;

import java.io.IOException;

import org.apache.http.conn.scheme.Scheme;
import org.jetbrains.annotations.TestOnly;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

/** Fluent API for building an image download request. */
public class RequestCreator {
  private final Picasso picasso;
  private final Request.Builder data;

  private boolean skipMemoryCache;
  private long fadeTime = Utils.FADE_DURATION;
  private boolean deferred;
  private int placeholderResId;
  private Drawable placeholderDrawable;
  private int errorResId;
  private Drawable errorDrawable;
  private long delayMillis;

  private int maxWidth = -1;
  private int maxHeight = -1;

  RequestCreator(Picasso picasso, Uri uri, int resourceId) {
    if (picasso.shutdown) {
      throw new IllegalStateException(
          "Picasso instance already shut down. Cannot submit new requests.");
    }
    this.picasso = picasso;
    this.data = new Request.Builder(uri, resourceId);
  }

  @TestOnly RequestCreator() {
    this.picasso = null;
    this.data = new Request.Builder(null, 0);
  }

  int getMaxWidth() {
    return maxWidth;
  }

  int getMaxHeight() {
    return maxHeight;
  }

  /**
   * A placeholder drawable to be used while the image is being loaded. If the requested image is
   * not immediately available in the memory cache then this resource will be set on the target
   * {@link ImageView}.
   */
  public RequestCreator placeholder(int placeholderResId) {
    if (placeholderResId == 0) {
      throw new IllegalArgumentException("Placeholder image resource invalid.");
    }
    if (placeholderDrawable != null) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    this.placeholderResId = placeholderResId;
    return this;
  }

  /**
   * A placeholder drawable to be used while the image is being loaded. If the requested image is
   * not immediately available in the memory cache then this resource will be set on the target
   * {@link ImageView}.
   * <p>
   * If you are not using a placeholder image but want to clear an existing image (such as when
   * used in an {@link android.widget.Adapter adapter}), pass in {@code null}.
   */
  public RequestCreator placeholder(Drawable placeholderDrawable) {
    if (placeholderResId != 0) {
      throw new IllegalStateException("Placeholder image already set.");
    }
    this.placeholderDrawable = placeholderDrawable;
    return this;
  }

  /** An error drawable to be used if the request image could not be loaded. */
  public RequestCreator error(int errorResId) {
    if (errorResId == 0) {
      throw new IllegalArgumentException("Error image resource invalid.");
    }
    if (errorDrawable != null) {
      throw new IllegalStateException("Error image already set.");
    }
    this.errorResId = errorResId;
    return this;
  }

  /** An error drawable to be used if the request image could not be loaded. */
  public RequestCreator error(Drawable errorDrawable) {
    if (errorDrawable == null) {
      throw new IllegalArgumentException("Error image may not be null.");
    }
    if (errorResId != 0) {
      throw new IllegalStateException("Error image already set.");
    }
    this.errorDrawable = errorDrawable;
    return this;
  }

  /**
   * Attempt to resize the image to fit exactly into the target {@link ImageView}'s bounds. This
   * will result in delayed execution of the request until the {@link ImageView} has been measured.
   * <p/>
   * <em>Note:</em> This method works only when your target is an {@link ImageView}.
   */
  public RequestCreator fit() {
    deferred = true;
    return this;
  }

  /**
   * Maximum width and height values the Bitmap can be sized to.
   * <p/>
   * <em>Note:</em> This method works only when your target is an {@link ImageView).
   */
  public RequestCreator maxSize(int maxWidth, int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    return this;
  }

  /** Internal use only. Used by {@link DeferredRequestCreator}. */
  RequestCreator unfit() {
    deferred = false;
    return this;
  }

  /**
   * Resizes the image to the specified dimensions
   * @param targetWidthResId target width
   * @param targetHeightResId target height
   * @see #resizeDimen(int,int,boolean)
   * @return
   */
  public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId) {
    return resizeDimen(targetWidthResId, targetHeightResId, false);
  }


  /**
   * Resizes the image to the specified dimensions
   * @param targetWidthResId target width
   * @param targetHeightResId target height
   * @param onlyIfBigger If true the bitmap will be resized only only if bigger than targetWidth or targetHeight. If false the bitmap will be always resized
   * @return
   */
  public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId, boolean onlyIfBigger) {
    Resources resources = picasso.context.getResources();
    int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
    int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
    return resize(targetWidth, targetHeight, onlyIfBigger);
  }

  /**
   * Resizes the image to the specified size in pixels
   * @param targetWidthResId target width
   * @param targetHeightResId target height
   * @param onlyIfBigger If true the bitmap will be resized only only if bigger than targetWidth or targetHeight. If false the bitmap will be always resized
   * @return
   */  
  public RequestCreator resize(int targetWidth, int targetHeight, boolean onlyIfBigger) {
    data.resize(targetWidth, targetHeight, onlyIfBigger);
    return this;
  }
  
  public RequestCreator withOptions( BitmapFactory.Options options ) {
	  data.useOptions(options);
	  return this;
  }
  
  /**
   * Assign a custom {@link Generator} which will be used to decode the Uri,
   * only if the uri {@link Scheme} is {@link Picasso#SCHEME_CUSTOM}
   * @param generator
   * @return
   */
  public RequestCreator withGenerator( Generator generator ) {
	  data.setGenerator( generator );
	  return this;
  }
  
  /**
   * Temporary use a different cache instance
   * @param cache
   */
  public RequestCreator withCache( Cache cache ) {
	  data.setCache( cache );
	  return this;
  }

  /**
   * Crops an image inside of the bounds specified by {@link #resize(int, int)} rather than
   * distorting the aspect ratio. This cropping technique scales the image so that it fills the
   * requested bounds and then crops the extra.
   */
  public RequestCreator centerCrop() {
    data.centerCrop();
    return this;
  }

  /**
   * Centers an image inside of the bounds specified by {@link #resize(int, int)}. This scales
   * the image so that both dimensions are equal to or less than the requested bounds.
   */
  public RequestCreator centerInside() {
    data.centerInside();
    return this;
  }

  /** Rotate the image by the specified degrees. */
  public RequestCreator rotate(float degrees) {
    data.rotate(degrees);
    return this;
  }

  /** Rotate the image by the specified degrees around a pivot point. */
  public RequestCreator rotate(float degrees, float pivotX, float pivotY) {
    data.rotate(degrees, pivotX, pivotY);
    return this;
  }

  /**
   * Add a custom transformation to be applied to the image.
   * <p/>
   * Custom transformations will always be run after the built-in transformations.
   */
  // TODO show example of calling resize after a transform in the javadoc
  public RequestCreator transform(Transformation transformation) {
    data.transform(transformation);
    return this;
  }

  /**
   * Indicate that this action should not use the memory cache for attempting to load or save the
   * image. This can be useful when you know an image will only ever be used once (e.g., loading
   * an image from the filesystem and uploading to a remote server).
   */
  public RequestCreator skipMemoryCache() {
    skipMemoryCache = true;
    return this;
  }

  /** Disable brief fade in of images loaded from the disk cache or network. */
  public RequestCreator noFade() {
    fadeTime = 0;
    return this;
  }
  
  /** use a custom fade-in duration, instead of the default.
   * @see {@link Utils#FADE_DURATION}
   * @param ms
   * @return
   */
  public RequestCreator fade( long ms ) {
	fadeTime = ms;
	return this;
  }
  
  public RequestCreator withDelay( long millis ) {
	  delayMillis = millis;
	  return this;
  }

  /** Synchronously fulfill this request. Must not be called from the main thread. */
  public Bitmap get() throws IOException {
    checkNotMain();
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with get.");
    }
    if (!data.hasImage()) {
      return null;
    }

    Request finalData = picasso.transformRequest(data.build());
    String key = Utils.createKey(finalData);

    Action<Void> action = new GetAction(picasso, finalData, skipMemoryCache, fadeTime, key);
    return forRequest(picasso.context, picasso, picasso.dispatcher, data.getCache() != null ? data.getCache() : picasso.getCache(), picasso.stats,
        action, picasso.dispatcher.downloader).hunt();
  }

  /**
   * Asynchronously fulfills the request without a {@link ImageView} or {@link Target}. This is
   * useful when you want to warm up the cache with an image.
   */
  public void fetch() {
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with fetch.");
    }
    
    if (data.hasImage()) {
      Request finalData = picasso.transformRequest(data.build());
      String key = Utils.createKey(finalData);

      Action<Void> action = new FetchAction(picasso, finalData, skipMemoryCache, fadeTime, key);
      picasso.enqueueAndSubmit(action, delayMillis);
    }
  }

  /**
   * Asynchronously fulfills the request into the specified {@link Target}. In most cases, you
   * should use this when you are dealing with a custom {@link android.view.View View} or view
   * holder which should implement the {@link Target} interface.
   * <p>
   * Implementing on a {@link android.view.View View}:
   * <blockquote><pre>
   * public class ProfileView extends FrameLayout implements Target {
   *   {@literal @}Override public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
   *     setBackgroundDrawable(new BitmapDrawable(bitmap));
   *   }
   *
   *   {@literal @}Override public void onBitmapFailed() {
   *     setBackgroundResource(R.drawable.profile_error);
   *   }
   * }
   * </pre></blockquote>
   * Implementing on a view holder object for use inside of an adapter:
   * <blockquote><pre>
   * public class ViewHolder implements Target {
   *   public FrameLayout frame;
   *   public TextView name;
   *
   *   {@literal @}Override public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
   *     frame.setBackgroundDrawable(new BitmapDrawable(bitmap));
   *   }
   *
   *   {@literal @}Override public void onBitmapFailed() {
   *     frame.setBackgroundResource(R.drawable.profile_error);
   *   }
   * }
   * </pre></blockquote>
   * <p>
   * <em>Note:</em> This method keeps a weak reference to the {@link Target} instance and will be
   * garbage collected if you do not keep a strong reference to it. To receive callbacks when an
   * image is loaded use {@link #into(android.widget.ImageView, Callback)}.
   */
  public void into(Target target) {
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null.");
    }
    if (deferred) {
      throw new IllegalStateException("Fit cannot be used with a Target.");
    }

    Drawable drawable =
        placeholderResId != 0 ? picasso.context.getResources().getDrawable(placeholderResId)
            : placeholderDrawable;

    if (!data.hasImage()) {
      picasso.cancelRequest(target);
      target.onPrepareLoad(drawable);
      return;
    }

    Request finalData = picasso.transformRequest(data.build());
    String requestKey = createKey(finalData);

    if (!skipMemoryCache) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck(data.getCache() != null ? data.getCache() : picasso.getCache(), requestKey );
      if (bitmap != null) {
        picasso.cancelRequest(target);
        target.onBitmapLoaded(bitmap, MEMORY);
        return;
      }
    }

    target.onPrepareLoad(drawable);

    Action<Target> action = new TargetAction(picasso, target, finalData, skipMemoryCache, fadeTime, requestKey);
    picasso.enqueueAndSubmit(action, delayMillis);
  }

  /**
   * Asynchronously fulfills the request into the specified {@link ImageView}.
   * <p/>
   * <em>Note:</em> This method keeps a weak reference to the {@link ImageView} instance and will
   * automatically support object recycling.
   */
  public void into(ImageView target) {
    into(target, null);
  }

  /**
   * Asynchronously fulfills the request into the specified {@link ImageView} and invokes the
   * target {@link Callback} if it's not {@code null}.
   * <p/>
   * <em>Note:</em> The {@link Callback} param is a strong reference and will prevent your
   * {@link android.app.Activity} or {@link android.app.Fragment} from being garbage collected. If
   * you use this method, it is <b>strongly</b> recommended you invoke an adjacent
   * {@link Picasso#cancelRequest(android.widget.ImageView)} call to prevent temporary leaking.
   */
  public void into(ImageView target, Callback callback) {
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null.");
    }

    if (!data.hasImage()) {
      picasso.cancelRequest(target);
      PicassoDrawable.setPlaceholder(target, placeholderResId, placeholderDrawable);
      return;
    }

    if (deferred) {
      if (data.hasSize()) {
        throw new IllegalStateException("Fit cannot be used with resize.");
      }
      int measuredWidth = target.getMeasuredWidth();
      int measuredHeight = target.getMeasuredHeight();
      if (maxWidth != -1 && measuredWidth > maxWidth)
        measuredWidth = maxWidth;
      if (maxHeight != -1 && measuredHeight > maxHeight)
        measuredHeight = maxHeight;
      if (measuredWidth == 0 && measuredHeight == 0) {
        PicassoDrawable.setPlaceholder(target, placeholderResId, placeholderDrawable);
        picasso.defer(target,
          new DeferredRequestCreator(this, target, callback));
        return;
      }
      data.resize(measuredWidth, measuredHeight, false);
    }

    Request finalData = picasso.transformRequest(data.build());
    String requestKey = createKey(finalData);

    if (!skipMemoryCache) {
      Bitmap bitmap = picasso.quickMemoryCacheCheck( data.getCache() != null ? data.getCache() : picasso.getCache(), requestKey);
      if (bitmap != null) {
        picasso.cancelRequest(target);
        PicassoDrawable.setBitmap(target, picasso.context, bitmap, MEMORY, fadeTime,
            picasso.debugging);
        if (callback != null) {
          callback.onSuccess();
        }
        return;
      }
    }

    PicassoDrawable.setPlaceholder(target, placeholderResId, placeholderDrawable);

    Action<ImageView> action =
        new ImageViewAction(picasso, target, finalData, skipMemoryCache, fadeTime, errorResId,
            errorDrawable, requestKey, callback);

    picasso.enqueueAndSubmit(action, delayMillis);
  }
}
