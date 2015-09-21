/* 
 * [��������] Android ͼƬ�����
 * [����] xmobileapp�Ŷ�
 * [ʹ��˵��] ����SD���Ͻ���picĿ¼�Դ��ͼƬ
 * [�ο�����] http://code.google.com/p/androidslideshow/ 
 * [��ԴЭ��] MIT License (http://www.opensource.org/licenses/mit-license.php)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.xmobileapp.pictureviewer;

import com.xmobileapp.pictureviewer.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.Scroller;

public class PictureShow extends Activity {

	private PictureShowUtils mAllImages = new PictureShowUtils();
	boolean mLayoutComplete;
	boolean mPausing = false;
	PictureSlideView mPictureSlideView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		mPictureSlideView = (PictureSlideView) findViewById(R.id.grid);
		mPictureSlideView.requestFocus();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int sel = mPictureSlideView.mSelImageIndex;
		int count = mAllImages.getCount();

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			if (sel >= 1) {
				sel--;
				mPictureSlideView.ScrollUp();
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (sel < count - 1) {
				sel++;
				mPictureSlideView.scrollDown();
			}

			break;
		default:
			break;
		}

		mPictureSlideView.mSelImageIndex = sel;

		mPictureSlideView.mImageBlockManager.loadSelBlock(sel);
		mPictureSlideView.invalidate();

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onPause() {
		super.onPause();
		mPausing = true;
	}

	@Override
	public void onResume() {
		super.onResume();
		mPausing = false;
	}


	
	public static class PictureSlideView extends View {
		// ������Ϣ
		class LayoutSpec {
			LayoutSpec(int cols, int w, int h, int leftEdgePadding,
					int rightEdgePadding, int intercellSpacing) {
				mColumns = cols;
				mCellWidth = w;
				mCellHeight = h;
				mLeftEdgePadding = leftEdgePadding;
				mRightEdgePadding = rightEdgePadding;
				mCellSpacing = intercellSpacing;
			}

			int mColumns;
			int mCellWidth, mCellHeight;
			int mLeftEdgePadding, mRightEdgePadding;
			int mCellSpacing;
		};

		private class ImageBlockManager {

			private int mBlockCacheFirstBlockNumber = 0;
			private int mBlockCacheStartOffset = 0;
			//����ͼ����
			private ImageBlock[] mBlockCache;
			//ѡ��������ͼ����
			private ImageBlock mSelBlock;
			//һ��������ͼ������
			private static final int sRowsPerPage = 4;
			//����ͼ�����С����
			private static final int sPagesPreCache = 2;
			private static final int sPagesPostCache = 2;
			// �Ŵ�Ԥ��ͼ�ĳߴ�
			private static final int sSelBlockWidth = 200;
			private static final int sSelBlockHeight = 200;

			private Bitmap mMissingImageThumbnailBitmap;

			ImageBlockManager(Context context) {
				mContext = context;

				mBlockCache = new ImageBlock[sRowsPerPage * (sPagesPreCache + sPagesPostCache + 1)];
				for (int i = 0; i < mBlockCache.length; i++) {
					mBlockCache[i] = new ImageBlock();
				}

				/* ��ȡͼƬ */
				loadBlocks();

				mSelBlock = new ImageBlock(sSelBlockWidth, sSelBlockHeight);

			}

			int blockHeight() {
				return mCurrentSpec.mCellSpacing + mCurrentSpec.mCellHeight
						+ mCurrentSpec.mCellSpacing;
			}

			int blockWidth() {
				return mCurrentSpec.mCellSpacing + mCurrentSpec.mCellWidth
						+ mCurrentSpec.mCellSpacing;
			}

			void loadSelBlock(int pos) {
				mSelBlock.loadSelImage(pos);
			}

			private void paintSelBlock(Canvas canvas) {
				int pos = mSelImageIndex;
				if (pos < 0)
					return;

				Bitmap b = mSelBlock.mBitmap;
				if (b == null) {
					return;
				}

				int xPos = 0;
				int yPos = 0;

				yPos = mScrollY + mCurrentSpec.mCellSpacing;

				xPos = mCurrentSpec.mCellWidth + mCurrentSpec.mLeftEdgePadding;

				canvas.drawBitmap(b, xPos, yPos, mGridViewPaint);
			}

			// ��ȡ����û������ͼ����ͼƬ����
			public Bitmap getErrorBitmap() {
				mMissingImageThumbnailBitmap = BitmapFactory.decodeResource(
						PictureSlideView.this.getResources(),
						R.drawable.nothumbnail);

				return mMissingImageThumbnailBitmap;
			}

			private void loadBlocks() {
				ImageBlock[] blocks = mBlockCache;
				int numBlocks = blocks.length;
				int first = 0;

				for (int i = 0; i < numBlocks; i++) {
					int j = first + i;
					if (j >= numBlocks)
						j -= numBlocks;
					ImageBlock b = blocks[j];
					b.mBlockNumber = j;
					if (b.loadBlock() == 0)
						break;
				}

			}

			void doDraw(Canvas canvas) {
				ImageBlockManager.ImageBlock[] blocks = mBlockCache;
				if (blocks[0] == null) {
					return;
				}

				final int thisHeight = getHeight();
				final int height = blocks[0].mBitmap.getHeight();
				final int scrollPos = mScrollY;
				int currentBlock = (scrollPos < 0) ? ((scrollPos - height + 1) / height)
						: (scrollPos / height);

				while (true) {

					final int yPos = currentBlock * height;
					if (yPos >= scrollPos + thisHeight) {
						break;
					}

					int effectiveOffset = (mBlockCacheStartOffset + (currentBlock++ - mBlockCacheFirstBlockNumber))
							% blocks.length;

					ImageBlock block = blocks[effectiveOffset];
					if (block == null)
						break;

					Bitmap b = block.mBitmap;
					if (b == null) {
						break;
					}
					canvas.drawBitmap(b, 0, yPos, mGridViewPaint);
				} // end of while

				/* Paint Selection */
				paintSelBlock(canvas);

			}/* end of doDraw */

			private class ImageBlock {

				private static final int mBlurRadius = 5;
				private static final int mBlockAlpha = 180;
				private static final int mBorderAlpha = 180;
				private static final int mSelBorderAlpha = 150;

				Drawable mCellOutline;

				Bitmap mBitmap;
				Canvas mCanvas;
				Paint mPaint = new Paint();

				int mBlockNumber;
				boolean mIsVisible;

				ImageBlock() {

					mBitmap = Bitmap.createBitmap(getWidth(), blockHeight(),
							Bitmap.Config.ARGB_8888);
					mCanvas = new Canvas(mBitmap);

					mPaint.setTextSize(14F);
					mPaint.setStyle(Paint.Style.FILL);
					mPaint.setAlpha(mBlockAlpha);

					mBlockNumber = -1;
					mCellOutline = PictureSlideView.this.getResources()
							.getDrawable(android.R.drawable.gallery_thumb);
					mCellOutline.setAlpha(mBorderAlpha);

				}

				ImageBlock(int w, int h) {

					mBitmap = Bitmap
							.createBitmap(w, h, Bitmap.Config.ARGB_8888);
					mCanvas = new Canvas(mBitmap);

					mPaint.setTextSize(14F);
					mPaint.setStyle(Paint.Style.FILL);
					mPaint.setFilterBitmap(true);
					mPaint.setMaskFilter(new BlurMaskFilter(mBlurRadius,
							BlurMaskFilter.Blur.NORMAL));

					mBlockNumber = -1;
					mCellOutline = PictureSlideView.this.getResources()
							.getDrawable(android.R.drawable.gallery_thumb);
					mCellOutline.setAlpha(mSelBorderAlpha);

				}

				private int loadBlock() {

					int count = mGallery.mAllImages.getCount();
					int base = (mBlockNumber * mCurrentSpec.mColumns);

					for (int col = 0; col < mCurrentSpec.mColumns; col++) {

						int spacing = mCurrentSpec.mCellSpacing;
						int leftSpacing = mCurrentSpec.mLeftEdgePadding;
						final int yPos = spacing;
						final int xPos = leftSpacing
								+ (col * (mCurrentSpec.mCellWidth + spacing));

						int pos = base + col;
						if (pos >= count)
							break;

						Bitmap b = mGallery.mAllImages.getImageAt(pos);
						if (b == null) {
							return 0;
						}
						drawBlockBitmap(b, xPos, yPos, mCurrentSpec.mCellWidth, mCurrentSpec.mCellHeight);
					}

					return 1;
				}

				private int loadSelImage(int pos) {

					final int yPos = 0;
					final int xPos = 0;

					Bitmap b = mGallery.mAllImages.getImageAt(pos);
					drawSelBlockBitmap(b, xPos, yPos, sSelBlockWidth,
							sSelBlockHeight);

					return 1;
				}

				private void drawBlockBitmap(Bitmap b, int left, int top,
						int width, int height) {
					mCanvas.setBitmap(mBitmap);

					if (b != null) {
						//����ͼƬ�Ĵ�С���вü������ţ���״��Ϊ�����Ρ�
						int w = width;
						int h = height;

						int bw = b.getWidth();
						int bh = b.getHeight();

						int deltaW = bw - w;
						int deltaH = bh - h;

						if (deltaW < 10 && deltaH < 10) {
							int halfDeltaW = deltaW / 2;
							int halfDeltaH = deltaH / 2;
							android.graphics.Rect src = new android.graphics.Rect(
									0 + halfDeltaW, 0 + halfDeltaH, bw
											- halfDeltaW, bh - halfDeltaH);
							android.graphics.Rect dst = new android.graphics.Rect(
									left, top, left + w, top + h);
							mCanvas.drawBitmap(b, src, dst, mPaint);
						} else {
							int l = (bw - w) / 2;
							int t = (bh - h) / 2;

							android.graphics.Rect src = new android.graphics.Rect(
									l, t, l + w, t + h);
							android.graphics.Rect dst = new android.graphics.Rect(
									left, top, left + w, top + h);
							mCanvas.drawBitmap(b, src, dst, mPaint);

						}
					} else {
						//�������ͼ���ܻ�ã���ʹ��Ĭ�ϵĴ���ͼƬ����
						Bitmap error = mImageBlockManager.getErrorBitmap();
						int w = error.getWidth();
						int h = error.getHeight();
						Rect source = new Rect(0, 0, w, h);
						int l = (w - w) / 2 + left;
						int t = (h - h) / 2 + top;
						Rect dest = new Rect(l, t, l + w, t + h);
						mCanvas.drawBitmap(error, source, dest, mPaint);

					}
					int[] stateSet = EMPTY_STATE_SET;

					paintBorder(stateSet, left, top, left + width, top + height);
				}

				//����ѡ����ͼƬ
				private void drawSelBlockBitmap(Bitmap b, int left, int top,
						int width, int height) {
					mCanvas.setBitmap(mBitmap);
					mBitmap.eraseColor(0);

					if (b != null) {
						//����Ŀ�������εĳߴ����ͼƬ��С
						int w = width;
						int h = height;

						int bw = b.getWidth();
						int bh = b.getHeight();

						int deltaW = bw - w;
						int deltaH = bh - h;

						if (deltaW < 10 && deltaH < 10) {
							int halfDeltaW = deltaW / 2;
							int halfDeltaH = deltaH / 2;
							android.graphics.Rect src = new android.graphics.Rect(
									0 + halfDeltaW, 0 + halfDeltaH, bw
											- halfDeltaW, bh - halfDeltaH);
							android.graphics.Rect dst = new android.graphics.Rect(
									left, top, left + w, top + h);
							mCanvas.drawBitmap(b, src, dst, mPaint);
						} else {
							int l = (bw - w) / 2;
							int t = (bh - h) / 2;

							android.graphics.Rect src = new android.graphics.Rect(
									l, t, l + w, t + h);
							android.graphics.Rect dst = new android.graphics.Rect(
									left, top, left + w, top + h);
							mCanvas.drawBitmap(b, src, dst, mPaint);

						}
					} else {
						// ���û�л�ȡ��ͼƬ����������"ͼƬ������"��Ĭ��ͼƬ
						Bitmap error = mImageBlockManager.getErrorBitmap();
						int w = error.getWidth();
						int h = error.getHeight();
						Rect source = new Rect(0, 0, w, h);
						int l = (w - w) / 2 + left;
						int t = (h - h) / 2 + top;
						Rect dest = new Rect(l, t, l + w, t + h);
						mCanvas.drawBitmap(error, source, dest, mPaint);

					}
					int[] stateSet = EMPTY_STATE_SET;

					paintBorder(stateSet, left + 1, top + 1, left + width - 1,
							top + height - 1);
				}

				//���Ʊ߿�
				private void paintBorder(int[] stateSet, int l, int t, int r,
						int b) {
					mCellOutline.setState(stateSet);
					mCanvas.setBitmap(mBitmap);
					mCellOutline.setBounds(l, t, r, b);
					mCellOutline.draw(mCanvas);

				}

			}// end of class ImageBlock

		} // end of class ImageBlockManager

	
		
		private Context mContext;
		// �������Activity
		private PictureShow mGallery;
		// ���ڻ�ͼ��Paint����
		private Paint mGridViewPaint = new Paint();
		
		private ImageBlockManager mImageBlockManager;
		private LayoutSpec mCurrentSpec;
		private int mScrollY = 0;
		// ��������
		private Scroller mScroller = new Scroller(getContext());
		// �����ж��û�������GestureDetector����
		private GestureDetector mGestureDetector;
		// ѡ��ͼƬ������ֵ
		private int mSelImageIndex = 0;

		private void init(Context context) {
			mContext = context;
			mGridViewPaint.setColor(0xFF000000);
			mGallery = (PictureShow) context;

			setVerticalScrollBarEnabled(true);

			mGestureDetector = new GestureDetector(
					new SimpleOnGestureListener() {

						@Override
						public boolean onDown(MotionEvent e) {
							int touchSelIndex = computeSelImageIndex(e);

							if (touchSelIndex >= 0)
								mImageBlockManager.loadSelBlock(touchSelIndex);

							invalidate();

							return true;
						}

						@Override
						public void onShowPress(MotionEvent e) {
							super.onShowPress(e);
						}
					});
		}

		public PictureSlideView(Context context, AttributeSet attrs,
				int defStyle) {
			super(context, attrs, defStyle);
			init(context);
		}

		public PictureSlideView(Context context, AttributeSet attrs) {
			super(context, attrs);
			init(context);
		}

		public PictureSlideView(Context context) {
			super(context);
			init(context);
		}

		@Override
		public void onLayout(boolean changed, int left, int top, int right,
				int bottom) {
			super.onLayout(changed, left, top, right, bottom);

			if (mGallery.isFinishing() || mGallery.mPausing) {
				return;
			}

			mCurrentSpec = new LayoutSpec(1, 100, 100, 8, 8, 10);
			mGallery.mLayoutComplete = true;

			/* Create Image Manager */
			if (mImageBlockManager == null) {
				mImageBlockManager = new ImageBlockManager(mContext);
			}

		}

		@Override
		public boolean onTouchEvent(android.view.MotionEvent ev) {

			mGestureDetector.onTouchEvent(ev);
			return true;
		}

		int computeSelImageIndex(android.view.MotionEvent ev) {

			int spacing = mCurrentSpec.mCellSpacing;
			int leftSpacing = mCurrentSpec.mLeftEdgePadding;
			int bh = mImageBlockManager.blockHeight();

			int x = (int) ev.getX();
			int y = (int) ev.getY();
			int maxHeight = bh * 4;

			if (y > maxHeight)
				return -1;

			int row = (mScrollY + y - spacing)
					/ (mCurrentSpec.mCellHeight + spacing);
			int col = Math.min(mCurrentSpec.mColumns - 1, (x - leftSpacing)
					/ (mCurrentSpec.mCellWidth + spacing));
			return (row * mCurrentSpec.mColumns) + col;
		}

		void ScrollUp() {
			mScroller.startScroll(0, mScrollY, 0, -mImageBlockManager
					.blockHeight(), 20);
			computeScroll();
		}

		void scrollDown() {
			mScroller.startScroll(0, mScrollY, 0, mImageBlockManager
					.blockHeight(), 20);
			computeScroll();
		}


		//���㻬��
		@Override
		public void computeScroll() {
			if (mScroller != null) {
				boolean more = mScroller.computeScrollOffset();
				scrollTo(0, (int) mScroller.getCurrY());

				if (more)
					postInvalidate();
				else {
					mScroller.forceFinished(true);
				}
			} else {
				super.computeScroll();
			}

		}

		@Override
		public void scrollBy(int x, int y) {
			scrollTo(x, mScrollY + y);
		}

		@Override
		public void scrollTo(int x, int y) {
			mScrollY = y;
			super.scrollTo(x, y);
		}


		public void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			if (mImageBlockManager != null)
				mImageBlockManager.doDraw(canvas);
		}

	} // end of PictureSlideView

}
