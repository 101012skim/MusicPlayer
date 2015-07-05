/*
 * Copyright 2015 Andrea De Cesare
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andreadec.musicplayer.ui;

import android.graphics.*;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.*;

import com.andreadec.musicplayer.adapters.*;

public class DragDropTouchListener implements RecyclerView.OnItemTouchListener {
    final static int SCROLL_THRESHOLD = 50;
    final static int SCROLL_AMOUNT = 20;

    enum Action {ACTION_NONE, ACTION_DECIDING, ACTION_SORT, ACTION_REMOVE};

    private RecyclerView recyclerView;
    private ImageView overlay;
    private OnItemMovedListener listener;
    private int layoutHeaderId;
    private int handlerId;

    private Action actionInProgress = Action.ACTION_NONE;
    private int initX, initY;

    private MusicPlayerAdapter adapter;

    private View draggingView = null;
    private int adjustY, initialPosition, currentPosition;

    public DragDropTouchListener(final RecyclerView recyclerView, ImageView overlay, int layoutHeaderId, final int handlerId, OnItemMovedListener listener) {
        this.recyclerView = recyclerView;
        this.overlay = overlay;
        this.layoutHeaderId = layoutHeaderId;
        this.listener = listener;
        this.handlerId = handlerId;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if(actionInProgress==Action.ACTION_NONE) {
            if(e.getAction()==MotionEvent.ACTION_DOWN) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if(view!=null && isInside(view.findViewById(handlerId), (int)e.getRawX(), (int)e.getRawY())) {
                    dragStart(e.getX(), e.getY());
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private boolean isInside(View handler, int touchX, int touchY) {
        if(handler==null) return false;
        int[] location = new int[2];
        handler.getLocationOnScreen(location);
        return touchX > location[0] && touchX < location[0]+handler.getWidth() &&
                touchY > location[1] && touchY < location[1]+handler.getHeight();
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        int action = e.getAction();
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
            if(actionInProgress==Action.ACTION_SORT) {
                dragEnd();
            } else {
                deleteEnd((int)e.getX());
            }
        } else {
            if(actionInProgress==Action.ACTION_DECIDING) {
                if (Math.abs(initX - (int)e.getX()) > (Math.abs(initY-(int)e.getY()))) {
                    actionInProgress = Action.ACTION_REMOVE;
                } else {
                    actionInProgress = Action.ACTION_SORT;
                }
            }

            if(actionInProgress==Action.ACTION_SORT) {
                dragProgress((int) e.getY());
            } else {
                deleteProgress((int) e.getX());
            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }

    private void dragStart(float x, float y) {
        adapter = (MusicPlayerAdapter)recyclerView.getAdapter();

        draggingView = recyclerView.findChildViewUnder(x, y);
        adjustY = (int)(y-draggingView.getTop());
        initialPosition = recyclerView.getChildPosition(draggingView);
        currentPosition = initialPosition;

        // Copy view to overlay
        Bitmap bitmap = Bitmap.createBitmap(draggingView.getWidth(), draggingView.getHeight(), Bitmap.Config.ARGB_8888);
        draggingView.draw(new Canvas(bitmap));
        overlay.setImageBitmap(bitmap);
        overlay.setTop(0);
        overlay.setLeft(0);
        overlay.setTranslationY(y-adjustY);
        overlay.setTranslationX(0);

        draggingView.setVisibility(View.INVISIBLE);

        initX = (int)x;
        initY = (int)y;
        actionInProgress = Action.ACTION_DECIDING;
    }

    private void dragEnd() {
        overlay.setImageBitmap(null);
        draggingView.setVisibility(View.VISIBLE);
        actionInProgress = Action.ACTION_NONE;
        listener.onItemMoved(initialPosition, currentPosition);
    }

    private void dragProgress(int y) {
        overlay.setTranslationY(y - adjustY);

        if(y<SCROLL_THRESHOLD) {
            //int first = layoutManager.findFirstVisibleItemPosition();
            //if(first>0) recyclerView.scrollToPosition(first-1);
            recyclerView.scrollBy(0, -SCROLL_AMOUNT);
        } else if(y>recyclerView.getBottom()-SCROLL_THRESHOLD) {
            //recyclerView.scrollToPosition(layoutManager.findLastVisibleItemPosition()+1);
            recyclerView.scrollBy(0, SCROLL_AMOUNT);
        }

        View viewUnder = recyclerView.findChildViewUnder(10, y);
        if(viewUnder!=null && !viewUnder.equals(draggingView)) {
            int newPosition = recyclerView.getChildPosition(viewUnder);
            if(viewUnder.getId()==layoutHeaderId) return;
            adapter.swapItems(currentPosition, newPosition);
            currentPosition = newPosition;
        }
    }

    private void deleteProgress(int x) {
        overlay.setTranslationX(x);
    }

    private void deleteEnd(int x) {
        overlay.setImageBitmap(null);
        actionInProgress = Action.ACTION_NONE;
        int recyclerViewWidth = recyclerView.getWidth();
        if(recyclerViewWidth-x < recyclerViewWidth/2) {
            listener.onItemDeleted(adapter.deleteItem(currentPosition));
        } else {
            draggingView.setVisibility(View.VISIBLE);
        }
    }

    public interface OnItemMovedListener {
        void onItemMoved(int from, int to);
        void onItemDeleted(Object item);
    }
}