package com.kitchenkeeper.android;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static com.kitchenkeeper.android.KitchenStore.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class NativeKitchenTest {
    Context context; KitchenStore store;
    @Before public void setup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();store=new KitchenStore(context);store.getWritableDatabase().delete("records",null,null);}
    @After public void cleanup(){store.close();}
    @Test public void storageRecipesAndBatchesRemainCorrect()throws Exception{
        JSONObject milk=blankItem("grocery");put(milk,"name","Milk");put(milk,"quantity",2);put(milk,"expiry","2026-09-12");milk=store.update("items",milk);JSONObject stale=copy(milk);put(milk,"quantity",1);store.update("items",milk);try{store.update("items",stale);fail("Stale update must fail");}catch(IllegalArgumentException expected){}
        JSONObject s=blankShopping();put(s,"name","Milk");put(s,"quantity",3);put(s,"checked",1);put(s,"itemId",milk.optString("id"));store.update("shopping",s);assertEquals(1,store.restock());assertEquals(2,store.records("items").size());assertEquals(1,store.get(milk.optString("id"),"items").optDouble("quantity"),.001);assertEquals(0,store.restock());assertEquals(0,store.records("shopping").size());
        JSONObject recipe=blankRecipe();put(recipe,"title","My lentil soup");put(recipe,"ingredients","Lentils\nOnion");put(recipe,"method","My own recipe");put(recipe,"rating",5);put(recipe,"favorite",1);put(recipe,"triedOn","2026-09-10");recipe=store.update("recipes",recipe);store.close();store=new KitchenStore(context);assertEquals(5,store.get(recipe.optString("id"),"recipes").optInt("rating"));assertFalse(dateValid("2026-02-30"));assertFalse(safeUrl("javascript:alert(1)"));assertFalse(safeUrl("https://good.example@evil.example/"));
        JSONObject backup=store.exportData();assertFalse(backup.has("apiKey"));assertTrue(store.importData(backup,new HashSet<>()).contains("existing entries kept"));assertEquals(2,store.records("items").size());
        JSONObject corrupt=copy(backup);JSONObject invalid=blankItem("grocery");put(invalid,"name","Bad date");put(invalid,"expiry","2026-99-99");JSONObject valid=blankItem("grocery");put(valid,"name","Must roll back");corrupt.getJSONArray("items").put(valid).put(invalid);try{store.importData(corrupt,new HashSet<>());fail("Invalid import must fail");}catch(IllegalArgumentException expected){}assertEquals(2,store.records("items").size());
    }
    @Test public void exactBarcodesAndCitationGate()throws Exception{
        assertEquals("042100005264",KitchenApi.barcode("04252614",true));assertEquals("3017620422003",KitchenApi.barcode("3017 6204 22003",false));assertTrue(KitchenApi.same("012345678905","0012345678905"));assertFalse(KitchenApi.same("3017620422003","012345678905"));try{KitchenApi.barcode("3017620422004",false);fail();}catch(Exception expected){}
        JSONObject unsupported=j("output",new JSONArray().put(j("type","message","content",new JSONArray().put(j("type","output_text","text","An unsupported claim")))));try{KitchenApi.evidence(unsupported,"recipes");fail("No search/citations must fail");}catch(Exception expected){}
        String text="Food guidance [1]";JSONObject cite=j("type","url_citation","start_index",14,"end_index",17,"title","FoodSafety.gov","url","https://www.foodsafety.gov/food-safety-charts");JSONObject evidence=j("output",new JSONArray().put(j("type","web_search_call","status","completed")).put(j("type","message","content",new JSONArray().put(j("type","output_text","text",text,"annotations",new JSONArray().put(cite))))));assertEquals(1,KitchenApi.evidence(evidence,"recipes").getJSONArray("sources").length());put(cite,"url","https://foodsafety.gov.evil.example/advice");try{KitchenApi.evidence(evidence,"recipes");fail("False source domain must fail");}catch(Exception expected){}
    }
    @Test public void actualKeystoreEncryptsAndOcrReadsLabel()throws Exception{
        PrivateKey key=new PrivateKey(context);String test="sk-test-only-not-a-real-api-key-4938";key.save(test);assertEquals(test,key.read());assertEquals("4938",key.hint());assertFalse(context.getSharedPreferences("private-ai",Context.MODE_PRIVATE).getString("ciphertext","").contains(test));key.clear();assertFalse(key.connected());
        Bitmap label=Bitmap.createBitmap(1100,700,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(label);canvas.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(58);canvas.drawText("TEST MILK",70,110,p);canvas.drawText("Ingredients: milk",70,230,p);canvas.drawText("Contains: milk",70,350,p);canvas.drawText("500 ml",70,470,p);
        com.google.mlkit.vision.text.TextRecognizer reader=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);String text=Tasks.await(reader.process(InputImage.fromBitmap(label,0)),45,TimeUnit.SECONDS).getText();reader.close();label.recycle();assertTrue(text.toLowerCase(Locale.ROOT).contains("milk"));JSONObject draft=KitchenApi.textDraft(text,"grocery");assertFalse(draft.optString("labelText").isEmpty());assertEquals("",draft.optString("expiry"));assertTrue(draft.optString("packageSize").contains("500"));
    }
    @Test public void nativePhotoBackupRestoresActualImageBytes()throws Exception{
        Photos photos=new Photos(context);Bitmap image=Bitmap.createBitmap(240,320,Bitmap.Config.ARGB_8888);image.eraseColor(Color.GREEN);File picked=new File(context.getCacheDir(),"test-photo.jpg");try(FileOutputStream stream=new FileOutputStream(picked)){image.compress(Bitmap.CompressFormat.JPEG,90,stream);}image.recycle();String id=photos.add(android.net.Uri.fromFile(picked));JSONObject item=blankItem("equipment");put(item,"name","Photo test pan");put(item,"photo",id);store.update("items",item);JSONObject backup=photos.export(store.exportData());assertTrue(backup.getJSONObject("photos").has(id));byte[] original=Photos.read(new FileInputStream(photos.file(id)),4000000);photos.remove(Collections.singleton(id));assertFalse(photos.exists(id));Set<String> imported=photos.importPhotos(backup);assertTrue(imported.contains(id));assertArrayEquals(original,Photos.read(new FileInputStream(photos.file(id)),4000000));assertTrue(photos.importPhotos(backup).isEmpty());assertEquals(id,backup.getJSONArray("items").getJSONObject(0).getString("photo"));photos.remove(imported);picked.delete();
    }
    @Test public void nativeUiSavesRecordsAndSurvivesRecreation()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
          onView(withText("Your kitchen,\nat a glance.")).check(matches(isDisplayed()));
          scenario.onActivity(activity->{assertNoBrowserView(activity.getWindow().getDecorView());assertEquals("com.kitchenkeeper.android",activity.getPackageName());});
          onView(allOf(withText("Food"),isDisplayed())).perform(click());onView(withText("Add grocery")).perform(click());onView(withTagValue(is((Object)"name"))).perform(scrollTo(),replaceText("Native test milk"),closeSoftKeyboard());scenario.recreate();onView(withTagValue(is((Object)"name"))).check(matches(withText("Native test milk")));onView(withText("Save item")).perform(scrollTo(),click());onView(withText("Native test milk")).check(matches(isDisplayed()));
          onView(allOf(withText("Recipes"),isDisplayed())).perform(click());onView(withText("Save a recipe")).perform(click());onView(withTagValue(is((Object)"title"))).perform(scrollTo(),replaceText("My first native recipe"),closeSoftKeyboard());onView(withTagValue(is((Object)"method"))).perform(scrollTo(),replaceText("My own method, saved offline."),closeSoftKeyboard());onView(withText("Save recipe")).perform(scrollTo(),click());onView(withText("My first native recipe")).check(matches(isDisplayed()));capture("native-recipes.png");
          onView(allOf(withText("Kitchen"),isDisplayed())).perform(click());capture("native-kitchen.png");
          onView(allOf(withText("Food"),isDisplayed())).perform(click());onView(withText("Add grocery")).perform(click());onView(withTagValue(is((Object)"name"))).perform(scrollTo(),replaceText("A reviewed product draft"),closeSoftKeyboard());capture("native-editor.png");
        }
        assertEquals(1,store.records("items").size());assertEquals(1,store.records("recipes").size());
    }
    private void assertNoBrowserView(View view){assertFalse(view instanceof android.webkit.WebView);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertNoBrowserView(((ViewGroup)view).getChildAt(i));}
    private void capture(String name)throws Exception{InstrumentationRegistry.getInstrumentation().waitForIdleSync();Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);File directory=new File(context.getExternalFilesDir(null),"screenshots");directory.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(directory,name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
}
