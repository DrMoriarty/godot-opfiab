package org.godotengine.godot;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import java.lang.IllegalStateException;

import org.onepf.opfutils.OPFLog;
import org.onepf.opfiab.OPFIab;
import org.onepf.opfiab.api.AdvancedIabHelper;
import org.onepf.opfiab.billing.BillingProvider;
import org.onepf.opfiab.listener.BillingListener;
import org.onepf.opfiab.listener.DefaultBillingListener;
import org.onepf.opfiab.model.Configuration;
import org.onepf.opfiab.model.Configuration.Builder;
import org.onepf.opfiab.model.billing.SkuType;
import org.onepf.opfiab.model.billing.Purchase;
import org.onepf.opfiab.model.billing.SkuDetails;
import org.onepf.opfiab.model.event.billing.ConsumeResponse;
import org.onepf.opfiab.model.event.billing.PurchaseResponse;
import org.onepf.opfiab.model.event.billing.InventoryResponse;
import org.onepf.opfiab.model.event.billing.SkuDetailsResponse;
import org.onepf.opfiab.sku.MapSkuResolver;
import org.onepf.opfiab.sku.TypedMapSkuResolver;
import org.onepf.opfiab.verification.SimplePublicKeyPurchaseVerifier;
import org.onepf.opfiab.verification.VerificationResult;

// Providers

//import org.onepf.opfiab.amazon.AmazonBillingProvider;

import org.onepf.opfiab.google.GoogleBillingProvider;
import org.onepf.opfiab.google.SimpleGooglePurchaseVerifier;

//import org.onepf.opfiab.openstore.ApplandBillingProvider;
//import org.onepf.opfiab.openstore.AptoideBillingProvider;
//import org.onepf.opfiab.openstore.SlideMEBillingProvider;
//import org.onepf.opfiab.openstore.YandexBillingProvider;
//import org.onepf.opfiab.openstore.OpenStoreBillingProvider;

//import org.onepf.opfiab.samsung.BillingMode;
//import org.onepf.opfiab.samsung.SamsungBillingProvider;
//import org.onepf.opfiab.samsung.SamsungMapSkuResolver;
//import org.onepf.opfiab.samsung.SamsungPurchaseVerifier;

public class GodotOpfIab extends Godot.SingletonBase {
    //variable
    private Activity activity = null;
    private int instanceId = 0;
    private Toast toast;
    private final String TAG = "OpfIab";
    private boolean debugMode = false;

    private HashMap<String, String> callbackFunctions;
    private Configuration configuration = null;
    private AdvancedIabHelper helper = null;
    private List<BillingProvider> providers = new ArrayList<BillingProvider>();
    private List<Purchase> purchases = new ArrayList<Purchase>();
    private List<SkuDetails> detailsList = new ArrayList<SkuDetails>();

    protected Dictionary dictionaryFromSkuDetails(SkuDetails details) {
        Dictionary item = new Dictionary();
        item.put("sku", details.getSku());
        item.put("type", details.getType().toString());
        if(details.getProviderName() != null) item.put("provider", details.getProviderName());
        if(details.getTitle() != null) item.put("title", details.getTitle());
        if(details.getPrice() != null) item.put("price", details.getPrice());
        if(details.getDescription() != null) item.put("description", details.getDescription());
        return item;
    }

    protected Dictionary dictionaryForSku(String sku) {
        boolean found = false;
        for(SkuDetails details: detailsList) {
            if(details.getSku() == sku) {
                return dictionaryFromSkuDetails(details);
            }
        }
        Dictionary item = new Dictionary();
        item.put("sku", sku);
        return item;
    }

    final BillingListener myListener = new DefaultBillingListener() {
            /*
            @Override
            public void onSetupStarted(@NonNull final SetupStartedEvent setupStartedEvent) {
                super.onSetupStarted(setupStartedEvent);
                Log.i(TAG, "onSetupStarted");
            }

            @Override
            public void onSetupResponse(@NonNull final SetupResponse setupResponse) {
                super.onSetupResponse(setupResponse);
                Log.i(TAG, "onSetupResponse");
            }

            @Override
            public void onRequest(@NonNull final BillingRequest billingRequest) {
                super.onRequest(billingRequest);
                Log.i(TAG, "onRequest");
            }

            @Override
            public void onResponse(@NonNull final BillingResponse billingResponse) {
                super.onResponse(billingResponse);
                Log.i(TAG, "onResponse");
            }
            */

            @Override
            public void onConsume(@NonNull final ConsumeResponse consumeResponse) {
                super.onConsume(consumeResponse);
                Log.i(TAG, "onConsume");
                if (consumeResponse.isSuccessful()) {
                    String sku = consumeResponse.getPurchase().getSku();
                    runCallback("consumed", dictionaryForSku(sku));
                } else {
                    runCallback("consumed", null);
                }
            }

            @Override
            public void onPurchase(@NonNull final PurchaseResponse purchaseResponse) {
                super.onPurchase(purchaseResponse);
                Log.i(TAG, "onPurchase");
                if(purchaseResponse.isSuccessful()) {
                    purchases.add(purchaseResponse.getPurchase());
                    String sku = purchaseResponse.getPurchase().getSku();
                    runCallback("purchased", dictionaryForSku(sku));
                } else {
                    runCallback("purchased", null);
                }
            }

            @Override
            public void onInventory(@NonNull final InventoryResponse inventoryResponse) {
                super.onInventory(inventoryResponse);
                Log.i(TAG, "onInventory");
                Map<Purchase, VerificationResult> inventory = inventoryResponse.getInventory();
                if(inventory != null) {
                    for(Purchase p: inventory.keySet()) {
                        VerificationResult res = inventory.get(p);
                        if(res == VerificationResult.SUCCESS) {
                            String sku = p.getSku();
                            runCallback("owned", dictionaryForSku(sku));
                        }
                    }
                }
            }

            @Override
            public void onSkuDetails(@NonNull final SkuDetailsResponse skuDetailsResponse) {
                super.onSkuDetails(skuDetailsResponse);
                Log.i(TAG, "onSkuDetails");
                Collection<SkuDetails> set = skuDetailsResponse.getSkusDetails();
                if(set != null) {
                    for(SkuDetails details: set) {
                        detailsList.add(details);
                        runCallback("details", dictionaryFromSkuDetails(details));
                    }
                }
            }
        };

    static public Godot.SingletonBase initialize(Activity p_activity) {
        return new GodotOpfIab(p_activity);
    }

    //constructor
    public GodotOpfIab(Activity p_activity) {
        //The registration of this and its functions
        registerClass("OpfIab", new String[]{
                "init", "registerCallback", "unregisterCallback",
                "purchase", "consume", "inventory", "skuDetails",
                "googleProvider", "amazonProvider", "samsungProvider", "yandexProvider",
                "aptoideProvider", "applandProvider", "slidemeProvider", "openStoreProvider"
        });
        callbackFunctions = new HashMap<String, String>();
        activity = p_activity;
    }


    // Register callbacks to GDscript
    public void registerCallback(final String callback_type, final String callback_function) {
        callbackFunctions.put(callback_type, callback_function);
    }

    // Deregister callbacks to GDscript
    public void unregisterCallback(final String callback_type) {
        callbackFunctions.remove(callback_type);
    }

    // Run a callback to GDscript
    private void runCallback(final String callback_type, final Object argument) {
        if (callbackFunctions.containsKey(callback_type)) {
            GodotLib.calldeferred(instanceId, callbackFunctions.get(callback_type), new Object[]{ argument });
        }
    }

    private MapSkuResolver skuMapResolver(final Dictionary skuMap) {
        final MapSkuResolver skuResolver = new MapSkuResolver();
        if(skuMap == null || skuMap.get_keys().length <= 0)
            return skuResolver;
        for(String key: skuMap.get_keys()) {
            try {
                // val must be a String
                String val = skuMap.get(key).toString();
                skuResolver.add(key, val);
            } catch (Exception e) {
            }
        }
        return skuResolver;
    }

    private TypedMapSkuResolver typedSkuMapResolver(final Dictionary skuMap) {
        final TypedMapSkuResolver skuResolver = new TypedMapSkuResolver();
        if(skuMap == null || skuMap.get_keys().length <= 0)
            return skuResolver;
        for(String key: skuMap.get_keys()) {
            try {
                Object val = skuMap.get(key);
                if(val == null) {
                    skuResolver.add(key, null, SkuType.CONSUMABLE);
                    Log.i(TAG, "Add consumable sku "+key+" without alias");
                } else if(val instanceof String) {
                    // val may be a String
                    skuResolver.add(key, (String)val, SkuType.CONSUMABLE);
                    Log.i(TAG, "Add consumable sku "+key+" with alias "+val.toString());
                } else {
                    // or val may be a Dictionary
                    Dictionary d = (Dictionary)val;
                    String sku = d.get("sku").toString();
                    String skuType = d.get("type").toString();
                    SkuType st = SkuType.CONSUMABLE;
                    if(skuType == "entitlement") st = SkuType.ENTITLEMENT;
                    else if(skuType == "subscription") st = SkuType.SUBSCRIPTION;
                    skuResolver.add(key, sku, st);
                    Log.i(TAG, "Add sku "+key+" with alias "+sku+" and type "+skuType);
                }
            } catch (Exception e) {
                Log.e(TAG, "Can not process sku map item: "+key);
            }
        }
        return skuResolver;
    }
    /*
    private SamsungMapSkuResolver samsungSkuMapResolver(final String groupId, final Dictionary skuMap) {
        final SamsungMapSkuResolver skuResolver = new SamsungMapSkuResolver(groupId);
        if(skuMap == null || skuMap.get_keys().length <= 0)
            return skuResolver;
        for(String key: skuMap.get_keys()) {
            try {
                Object val = skuMap.get(key);
                if(val instanceof String) {
                    // val may be a String
                    skuResolver.add(key, (String)val, SkuType.CONSUMABLE);
                } else {
                    // or val may be a Dictionary
                    Dictionary d = (Dictionary)val;
                    String sku = d.get("sku").toString();
                    String skuType = d.get("type").toString();
                    SkuType st = SkuType.CONSUMABLE;
                    if(skuType == "entitlement") st = SkuType.ENTITLEMENT;
                    else if(skuType == "subscription") st = SkuType.SUBSCRIPTION;
                    skuResolver.add(key, sku, st);
                }
            } catch (Exception e) {
            }
        }
        return skuResolver;
    }
    */
    public void googleProvider(final String storeKey, final Dictionary skuMap) {
        providers.add(new GoogleBillingProvider.Builder(activity)
                      .setPurchaseVerifier(new SimpleGooglePurchaseVerifier(storeKey))
                      .setSkuResolver(typedSkuMapResolver(skuMap))
                      .build());
    }

    public void amazonProvider(final Dictionary skuMap) {
        /*
        providers.add(new AmazonBillingProvider.Builder(activity)
                      .setSkuResolver(skuMapResolver(skuMap))
                      .build());
        */
    }

    public void samsungProvider(final String groupId, final Dictionary skuMap) {
        /*
        providers.add(new SamsungBillingProvider.Builder(activity)
                      .setBillingMode(BillingMode.TEST_SUCCESS)
                      .setPurchaseVerifier(new SamsungPurchaseVerifier(activity, BillingMode.TEST_SUCCESS))
                      .setSkuResolver(samsungSkuMapResolver(groupId, skuMap))
                      .build());
        */
    }

    public void yandexProvider(final String storeKey, final Dictionary skuMap) {
        /*
        providers.add(new OpenStoreBillingProvider.Builder(activity)
                      .setPurchaseVerifier(new SimplePublicKeyPurchaseVerifier(storeKey))
                      .setSkuResolver(typedSkuMapResolver(skuMap))
                      .build());
        */
    }

    public void aptoideProvider(final String storeKey, final Dictionary skuMap) {
        /*
        providers.add(new AptoideBillingProvider.Builder(activity)
                      .setPurchaseVerifier(new SimplePublicKeyPurchaseVerifier(storeKey))
                      .setSkuResolver(typedSkuMapResolver(skuMap))
                      .build());
        */
    }

    public void applandProvider(final Dictionary skuMap) {
        /*
        providers.add(new ApplandBillingProvider.Builder(activity)
                      .setSkuResolver(typedSkuMapResolver(skuMap))
                      .build());
        */
    }

    public void slidemeProvider(final String storeKey, final Dictionary skuMap) {
        /*
        providers.add(new SlideMEBillingProvider.Builder(activity)
                      .setPurchaseVerifier(new SimplePublicKeyPurchaseVerifier(storeKey))
                      .setSkuResolver(typedSkuMapResolver(skuMap))
                      .build());
        */
    }

    public void openStoreProvider(final Dictionary skuMap) {
        /*
        providers.add(new OpenStoreBillingProvider.Builder(activity)
                      .setSkuResolver(typedSkuMapResolver(skuMap))
                      .build());
        */
    }

    //initialization of OpfIab
    public void init(final int new_instanceId, final boolean debugMode) {
        instanceId = new_instanceId;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(debugMode)
                    OPFLog.setEnabled(true, true);
                Builder builder = new Configuration.Builder();
                builder.setAutoRecover(true);
                builder.setSkipStaleRequests(false);
                for(BillingProvider provider: providers) {
                    builder.addBillingProvider(provider);
                }
                builder.setBillingListener(myListener);
                configuration = builder.build();
                OPFIab.init(activity.getApplication(), configuration);
                helper = OPFIab.getAdvancedHelper();
                helper.register();
            }
        });
    }

    public void purchase(final String sku) {
        if(helper != null) {
            activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        helper.purchase(activity, sku);
                    }
                });
        }
    }

    public void consume(final String sku) {
        if(helper != null) {
            for(final Purchase p: purchases) {
                if(p.getSku() == sku) {
                    activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                helper.consume(activity, p);
                            }
                        });
                    return;
                }
            }
        }
        Log.e(TAG, "Purchase "+sku+" not found!");
    }

    public void inventory() {
        if(helper != null) {
            activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        helper.inventory(activity, true);
                    }
                });
        }
    }

    public void skuDetails(final String[] skus) {
        List<String> newSkus = new ArrayList<String>();
        for(String sku: skus) {
            boolean found = false;
            for(SkuDetails details: detailsList) {
                if(details.getSku() == sku) {
                    found = true;
                    runCallback("details", dictionaryFromSkuDetails(details));
                }
            }
            if(!found)
                newSkus.add(sku);
        }
        final HashSet<String> skuSet = new HashSet<String>(newSkus); 
        if(helper != null) {
            activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        helper.skuDetails(activity, skuSet);
                    }
                });
        }
    }

    @Override protected void onMainActivityResult (int requestCode, int resultCode, Intent data)
    {
        if(helper != null)
            helper.onActivityResult(activity, requestCode, resultCode, data);
    }
    
    @Override protected void onMainDestroy() {
        if(helper != null)
            helper.unregister();
    }
}
