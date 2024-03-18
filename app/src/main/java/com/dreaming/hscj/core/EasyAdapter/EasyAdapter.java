package com.dreaming.hscj.core.EasyAdapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


/**
 * @作者 宋旭升
 */
public class EasyAdapter<T> extends RecyclerView.Adapter<EasyViewHolder>{
    final static String TAG = "EasyAdapter";
    private int a = 0;

    public void onItemDismiss(int position) {
        Log.e(TAG,position+"->onItemDismiss");
        lstDatas.remove(position);
        notifyItemRemoved(position);
    }

    public interface IEasyAdapter<T>{
        void convert(EasyViewHolder holder, T data, int position);
    }
    private Context context;
    private int layoutResID;
    private boolean bIsUseCache = true;
    private IEasyAdapter iEasyAdapter;
    private List<T> lstDatas;
    private Handler handler = new Handler();

    private void convert(EasyViewHolder holder, T data, int position){
        iEasyAdapter.convert(holder,data,position);
    }

    private EasyAdapter(Context context, @LayoutRes int layoutResID, List<T> lstDatas){
        this.context = context;
        this.layoutResID = layoutResID;
        this.lstDatas = lstDatas;
    }

    public EasyAdapter(Context context, @LayoutRes int layoutResID, List<T> lstDatas, IEasyAdapter<T> iEasyAdapter){
        this(context,layoutResID,lstDatas);
        this.iEasyAdapter = iEasyAdapter;
    }

    public EasyAdapter(Context context, @LayoutRes int layoutResID, List<T> lstDatas, IEasyAdapter<T> iEasyAdapter, boolean isUseCache){
        this(context,layoutResID,lstDatas,iEasyAdapter);
        setCacheEnabled(isUseCache);
    }

    boolean isDragEnable = false;
    public EasyAdapter setDragEnabled(boolean isDragEnable){
        this.isDragEnable=isDragEnable;
        return this;
    }

    boolean isDragDelete = true;
    public EasyAdapter setEnableDragDelete(boolean isEnable){
        isDragDelete = isEnable;
        return this;
    }

    public EasyAdapter setCacheEnabled(boolean isUserCache){
        setHasStableIds(!(bIsUseCache=isUserCache));
        return this;
    }
    boolean isEnableLoadAnim = true;
    public EasyAdapter setAnimLoadEnabled(boolean isEnableLoadAnim){
        this.isEnableLoadAnim = isEnableLoadAnim;
        return this;
    }

    @Override
    public EasyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EasyViewHolder(LayoutInflater.from(context).inflate(layoutResID,parent,false));
    }

    @Override
    public void onBindViewHolder(EasyViewHolder holder, int position) {
        handler.post(()->{ try { convert(holder, lstDatas.get(position),position); } catch (Exception e) {e.printStackTrace();
//            Log.e("EasyAdapter",e.getMessage()+"");
        } });
    }

    private volatile EasyItemTouchHelperCallback itemTouchHelperCallback;
    private final Object lockerOfItemTouchHelperCallback = new Object();
    public EasyItemTouchHelperCallback getTouchHelperCallback(){
        if(itemTouchHelperCallback == null){
            synchronized (lockerOfItemTouchHelperCallback){
                if(itemTouchHelperCallback==null){
                    itemTouchHelperCallback = new EasyItemTouchHelperCallback(this);
                }
            }
        }
        return itemTouchHelperCallback;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        handler.removeCallbacksAndMessages(null);
    }

    RecyclerView recyclerView;
    public RecyclerView getRecyclerView(){
        return recyclerView;
    }
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        if(!bIsUseCache&&recyclerView!=null){
            recyclerView.getRecycledViewPool().setMaxRecycledViews(0,0);
        }
        if(isDragEnable){
            ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(getTouchHelperCallback());
            mItemTouchHelper.attachToRecyclerView(recyclerView);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return lstDatas==null?0:lstDatas.size();
    }

    class EasyItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private EasyAdapter mAdapter;
        private EasyItemTouchHelperCallback(EasyAdapter mAdapter) {
            this.mAdapter = mAdapter;
        }

        /**拖拽行为设置*/
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            //上下拖拽
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            //向右侧滑
            int swipeFlags = ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, isDragDelete ? swipeFlags : 0);
        }
        /**拖拽中*/
        private int fromPosition, toPosition ;
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            fromPosition = viewHolder.getAdapterPosition();
            toPosition = target.getAdapterPosition();

            return fromPosition!=toPosition;
        }
        /**右滑删除*/
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            if(isDragDelete){
                mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
            }
            else{
                mAdapter.notifyItemInserted(viewHolder.getAdapterPosition());
            }
        }
        /**选择开始*/
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                if (viewHolder instanceof EasyViewHolder) {
                    ((EasyViewHolder) viewHolder).onItemSelected();
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }
        /**选择结束*/
        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (viewHolder instanceof EasyViewHolder) {
                ((EasyViewHolder) viewHolder).onItemClear();
            }
            if(fromPosition>toPosition){
                int temp = toPosition;
                toPosition = fromPosition;
                fromPosition = temp;
            }
            try {
                fromPosition = Math.max(0,fromPosition);
                toPosition   = Math.min(toPosition+1,lstDatas.size());
                mAdapter.lstDatas.add(toPosition,mAdapter.lstDatas.get(fromPosition));
                mAdapter.lstDatas.remove(fromPosition);
                mAdapter.notifyItemRangeChanged(fromPosition,toPosition);
            } catch (Exception e) {
            }
            if(!bIsUseCache){
                mAdapter.notifyDataSetChanged();
            }
        }
        /**是否允许拖拽*/
        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }
        /**是否长按以开始拖拽*/
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }

}