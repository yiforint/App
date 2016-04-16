package com.leo.appmaster.ad;

import android.content.Context;
import android.view.View;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.applocker.manager.ADShowTypeRequestManager;
import com.leo.appmaster.applocker.manager.MobvistaEngine;
import com.leo.appmaster.db.PrefTableHelper;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.utils.LeoLog;
import com.mobvista.msdk.out.Campaign;
import com.mobvista.msdk.out.MvNativeHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by stone on 16/2/26.
 */
public class ADEngineWrapper {

    private static final String TAG = "ADEngineWrapper [AD_DEBUG]";
	
	public static final int AD_TYPE_NATIVE = 0;
	public static final int AD_TYPE_TEMPLATE = 1;

    /* 两个可选的广告来源 */
    public static final int SOURCE_MOB = AppMasterPreference.AD_SDK_SOURCE_USE_3TH;
    public static final int SOURCE_MAX = AppMasterPreference.AD_SDK_SOURCE_USE_MAX;

    private LEOAdEngine mMaxEngine;
    private MobvistaEngine mMobEngine;
	
    private Map<String, Iterator<Integer>> mRandomMap = new HashMap<String, Iterator<Integer>>();

    public static interface WrappedAdListener {
        /**
         * 广告请求回调
         * @param code 返回码，如ERR_PARAMS_NULL
         * @param campaign 请求成功的广告结构体，失败为null
         * @param msg 请求失败sdk返回的描述，成功为null
         */
        public void onWrappedAdLoadFinished(int code, WrappedCampaign campaign, String msg, Object obj);
		
		public void onWrappedAdLoadFinished(int code, List<WrappedCampaign> campaignList,  String msg, Object obj, Object...flag);
        /**
         * 广告点击回调
         * @param campaign
         */
        public void onWrappedAdClick(WrappedCampaign campaign, String unitID);
    }

    private static ADEngineWrapper sInstance;
    private ADEngineWrapper (Context context) {
        mMaxEngine = LEOAdEngine.getInstance(context);
        mMobEngine = MobvistaEngine.getInstance(context);
    }

    public static ADEngineWrapper getInstance (Context context) {
        if (sInstance == null) {
            sInstance = new ADEngineWrapper(context.getApplicationContext());
        }
        return sInstance;
    }
	
	public MvNativeHandler getMvNativeHandler(String unitId, int adType) {
		if(adType == AD_TYPE_NATIVE) {
			return mMobEngine.getMvNativeHandler(unitId);
		} else {
			return mMobEngine.getTemplateHandler(unitId);
		}
	}

    /**
     * 批量加载广告，目前仅仅用于Lock页三个大图, 一次概率控制三个广告展示
     * @param sources
     * @param unitIds
     * @param listeners
     * @param forceLoad 不管概率如何，强制加载
     */
    public void loadAdBatch(int[] sources, String[] unitIds, WrappedAdListener[] listeners, boolean forceLoad, MvNativeHandler...mvNativeHandler) {
        if (sources == null || sources.length <= 0 || unitIds == null || unitIds.length <=0 || sources.length != unitIds.length) {
            return;
        }
        if (!forceLoad && !isHitProbability(unitIds[0])) {
            // 未命中显示概率，不显示广告
            if (listeners != null) {
                for (int i = 0; i < listeners.length; i++) {
                    WrappedAdListener listener = listeners[i];
                    if (listener == null) {
                        continue;
                    }
                    listener.onWrappedAdLoadFinished(LEOAdEngine.ERR_MOBVISTA_RESULT_NULL, null, null, null, "probability is not hit.");
                }
            }
            return;
        }
        for (int i = 0; i < sources.length; i++) {
            WrappedAdListener listener = null;
            try {
                listener = listeners[i];
            } catch (Exception e) {
                // 可以不用传listener进来，catch住所有异常忽略掉
            }
			if (sources[i] == SOURCE_MOB) {
				loadAdForce(sources[i], unitIds[i], listener, mvNativeHandler[i]);	
			} else {
				loadAdForce(sources[i], unitIds[i], listener, null);
			}
            
        }
    }
	
	


	private void loadMobTemplate(final String unitId, final WrappedAdListener listener) {
		if (unitId!= null && listener != null) {
			mMobEngine.loadMobvistaTemplate(unitId, new MobvistaEngine.MobvistaListener() {
				@Override
				public void onMobvistaFinished(int code, List<Campaign> campaigns,  String msg) {
					if (listener != null) {

						if (code == LEOAdEngine.ERR_OK) {
							List<WrappedCampaign> wrappedCampaignList = WrappedCampaign.fromMabVistaSDK(campaigns);
							listener.onWrappedAdLoadFinished(code, wrappedCampaignList,  msg, campaigns);
						} else {

							listener.onWrappedAdLoadFinished(code, null,null, msg, new Object[]{});
						}
					}
				}

				@Override
				public void onMobvistaClick(Campaign campaign, String unitID) {
					if (listener != null) {
						WrappedCampaign wrappedCampaign = WrappedCampaign.converCampaignFromMob(campaign);
						listener.onWrappedAdClick(wrappedCampaign, unitID);
					}
				}
			});
		}
	}

    /***
     * 请求广告数据
     */
    public void loadAd (final int source, final String unitId, final int adType, MvNativeHandler mvNativeHandler, final WrappedAdListener listener) {
        if (!isHitProbability(unitId)) {
            // 未命中显示概率，不显示广告
            if (listener != null) {
                listener.onWrappedAdLoadFinished(LEOAdEngine.ERR_MOBVISTA_RESULT_NULL, null, null, null, "probability is not hit.");
            }
            return;
        }
		
		if (source == AppMasterPreference.AD_SDK_SOURCE_USE_3TH && adType == AD_TYPE_TEMPLATE) {
			if (listener == null) {
				listener.onWrappedAdLoadFinished(LEOAdEngine.ERR_MOBVISTA_RESULT_NULL, null, null, null, "call back is null.");
				return;
			}
			loadMobTemplate(unitId, listener);
			return;
		}

		loadAdForce(source, unitId, listener, mvNativeHandler);
    }

    private void loadAdForce(final int source, final String unitId,  final WrappedAdListener listener, MvNativeHandler mvNativeHandler) {
		LeoLog.e(TAG, "AD TYPE :" + source + " AD ID: " + unitId);
		
		String sdk = (source == 2) ? "Max" : "Mobvista";
		final TreeMap<String, String> map = new TreeMap<String, String>();
		map.put("unitId", unitId);
		SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "start_to_loadad", sdk, source, map);
        
		if (source == SOURCE_MAX) {
            mMaxEngine.loadMobvista(unitId, new LEOAdEngine.LeoListener() {
                @Override
                public void onLeoAdLoadFinished(int code, LEONativeAdData campaign, String msg) {
                    LeoLog.d(TAG, "[" + unitId + "] source = " + source + "; code = " + code);
                    WrappedCampaign wrappedCampaign = null;
                    if (code == LEOAdEngine.ERR_OK) {
						wrappedCampaign = WrappedCampaign.converCampaignFromMax(campaign);

						if (wrappedCampaign != null) {
							map.put("msg", msg);
							SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "ad_onLoadFinished", "ready", source, map);
						} else {
							SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "ad_onLoadFinished", "null", source, map);
						}
                    } else {

						SDKWrapper.addEvent(AppMasterApplication.getInstance(), "max_ad", SDKWrapper.P1, "ad_onLoadFinished", "fail", source, map);
					}
                    listener.onWrappedAdLoadFinished(code, wrappedCampaign,  msg, null);
                }

                @Override
                public void onLeoAdClick(LEONativeAdData campaign, String unitID) {
                    listener.onWrappedAdClick(WrappedCampaign.converCampaignFromMax(campaign), unitID);
                }
            });
        } else {
			
			mMobEngine.loadMobvista(unitId, mvNativeHandler, new MobvistaEngine.MobvistaListener() {
				@Override
				public void onMobvistaFinished(int code, List<Campaign> campaign,  String msg) {
					LeoLog.d(TAG, "[" + unitId + "] source = " + source + "; code = " + code);
					WrappedCampaign wrappedCampaign = null;
					if (code == MobvistaEngine.ERR_OK && campaign != null && campaign.size() > 0) {
						wrappedCampaign = WrappedCampaign.converCampaignFromMob(campaign.get(0));
					}
					
					listener.onWrappedAdLoadFinished(code, wrappedCampaign,  msg, campaign);
				}

				@Override
				public void onMobvistaClick(Campaign campaign, String unitID) {
					listener.onWrappedAdClick(WrappedCampaign.converCampaignFromMob(campaign), unitID);
				}
			});

        }
    }

    /**
     * 此次随机是否命中展示概率
     * @return
     */
    private boolean isHitProbability(String unitId) {
        int local = nextRandomInt(unitId);
        int server = PrefTableHelper.getAdProbability();

        boolean hit = local < server;
        LeoLog.d(TAG, "[" + unitId + "] probability hit: " + hit + "; localRandom = " + local + "; server = " + server);
        return hit;
    }

    /**
     * 精准的随机，10次，0 ~ 9 每个数字都会被随机到
     * @param unitId
     * @return
     */
    private int nextRandomInt(String unitId) {
        synchronized (mRandomMap) {
            int max = ADShowTypeRequestManager.AD_PROBABILITY_MAX;
            Iterator<Integer> randomList = mRandomMap.get(unitId);
            if (randomList == null || !randomList.hasNext()) {
                List<Integer> list = new ArrayList<Integer>(max);
                for (int i = 0; i < max; i++) {
                    list.add(i);
                }
                // 随机打乱
                Collections.shuffle(list);
                StringBuilder stringBuilder = new StringBuilder();
                for (Integer integer : list) {
                    stringBuilder.append(integer).append(",");
                }
                LeoLog.d(TAG, "gen random list: " + stringBuilder.toString());
                randomList = list.iterator();
                mRandomMap.put(unitId, randomList);
            }

            int result = randomList.next();
            randomList.remove();
            return result;
        }
    }
	
	public void registerTemplateView(View view, Campaign campaign,  String unitId) {
		mMobEngine.registerTemplateView(view, campaign, unitId);
	}

    public void registerView (int source, View view, String unitId, Campaign campagin) {
        LeoLog.d(TAG, "registerView called");
        if (source == SOURCE_MAX) {
            mMaxEngine.registerView(unitId, view);
        } else {
            mMobEngine.registerView(unitId, view, campagin);
        }
    }
    public void releaseAd (int source, String unitId, View view, Campaign campaign, MvNativeHandler handler) {
        if (source == SOURCE_MAX) {
            mMaxEngine.release(unitId);
        } else {
            mMobEngine.release(unitId, view, campaign, handler);
        }
    }
	
	public void removeMobAdData(int source, String unitId) {
		if (source == SOURCE_MAX) {
			mMaxEngine.removeMobAdData(unitId);
		}
	}
	
	public boolean isADCacheEmpty(int source) {
		if (source == SOURCE_MAX) {
			return mMaxEngine.isADCacheEmpty();
		} else {
			return mMobEngine.isADCacheEmpty();
		}
	}
	
	public void preloadAppWall(String unitId) {
		mMobEngine.preloadAppWall(unitId);
	}
	
	/*public void proloadTemplate(String unitId) {
		mMobEngine.proloadTemplate(unitId);
	}
	
	public void releaseTemplateAd(String unitId) {
		mMobEngine.releaseTemplate(unitId);
	}*/
}
