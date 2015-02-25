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

package com.andreadec.musicplayer.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.andreadec.musicplayer.*;

public class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView text;
    private ListsClickListener clickListener;

    public HeaderViewHolder(View view, ListsClickListener clickListener) {
        super(view);
        this.clickListener = clickListener;
        text = (TextView)view.findViewById(R.id.textViewHeader);
        view.setOnClickListener(this);
    }

    public void update(String msg) {
        text.setText(msg);
    }

    @Override
    public void onClick(View view) {
        clickListener.onHeaderClick();
    }
}
