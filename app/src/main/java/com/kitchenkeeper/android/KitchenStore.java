package com.kitchenkeeper.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Native, transactional storage. Records and photos stay in this app's private storage. */
public final class KitchenStore extends SQLiteOpenHelper {
    public static final String[] DEFAULT_PLACES={"Fridge","Freezer","Pantry","Cabinets","Countertop"};
    public static final String[] UNITS={"pcs","packs","bottles","jars","cans","bags","boxes","kg","g","L","ml"};
    public static final String[] FOOD_CATEGORIES={"Vegetables","Fruit","Dairy & eggs","Meat & fish","Grains & pasta","Cans & jars","Spices & oils","Baking","Snacks","Drinks","Frozen","Other"};
    public static final String[] TOOL_CATEGORIES={"Appliances","Cookware","Bakeware","Utensils","Tableware","Storage","Other"};
    public KitchenStore(Context c){super(c,"kitchenkeeper.db",null,1);setWriteAheadLoggingEnabled(true);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY NOT NULL,type TEXT NOT NULL,body TEXT NOT NULL,updated INTEGER NOT NULL)");db.execSQL("CREATE INDEX records_type ON records(type)");db.execSQL("CREATE TABLE meta(key TEXT PRIMARY KEY NOT NULL,value TEXT NOT NULL)");}
    @Override public void onUpgrade(SQLiteDatabase db,int old,int next){throw new IllegalStateException("Unsupported database migration");}
    public static JSONObject j(Object...pairs){JSONObject o=new JSONObject();for(int i=0;i<pairs.length;i+=2)put(o,String.valueOf(pairs[i]),pairs[i+1]);return o;}
    public static void put(JSONObject o,String k,Object v){try{o.put(k,v);}catch(JSONException e){throw new IllegalArgumentException("Invalid field",e);}}
    public static JSONObject copy(JSONObject o){try{return new JSONObject(o.toString());}catch(JSONException e){throw new IllegalArgumentException("Invalid record",e);}}
    public static String id(){return UUID.randomUUID().toString();}
    public static JSONObject blankItem(String kind){JSONObject o=j("id",id(),"kind",kind,"name","","category",kind.equals("grocery")?"Other":"Appliances","location",kind.equals("grocery")?"Pantry":"Cabinets","quantity",1,"unit","pcs","minimum",0,"revision",0);for(String k:new String[]{"expiry","opened","brand","model","serial","purchased","warranty","notes","photo","barcode","packageSize","ingredients","allergens","nutrition","storageInstructions","labelText","sourceName","sourceUrl"})put(o,k,"");return o;}
    public static JSONObject blankRecipe(){return j("id",id(),"title","","ingredients","","method","","servings",2,"minutes",0,"triedOn","","rating",0,"favorite",0,"notes","","sourceUrl","","photo","","revision",0);}
    public static JSONObject blankShopping(){return j("id",id(),"name","","quantity",1,"unit","pcs","checked",0,"itemId","","location","Pantry","category","Other","revision",0);}
    public synchronized List<JSONObject> records(String type){List<JSONObject> out=new ArrayList<>();try(Cursor c=getReadableDatabase().query("records",new String[]{"body"},"type=?",new String[]{type},null,null,"updated DESC")){while(c.moveToNext())try{out.add(new JSONObject(c.getString(0)));}catch(JSONException e){throw new IllegalStateException("A saved record cannot be read",e);}}return out;}
    public synchronized JSONObject get(String id,String type){try(Cursor c=getReadableDatabase().query("records",new String[]{"body"},"id=? AND type=?",new String[]{id,type},null,null,null)){if(c.moveToFirst())try{return new JSONObject(c.getString(0));}catch(JSONException e){throw new IllegalStateException(e);}return null;}}
    public static boolean dateValid(String s){if(s.isEmpty())return true;try{return s.matches("\\d{4}-\\d{2}-\\d{2}")&&LocalDate.parse(s).toString().equals(s);}catch(Exception e){return false;}}
    public static long days(String date){try{return ChronoUnit.DAYS.between(LocalDate.now(),LocalDate.parse(date));}catch(Exception e){return Long.MAX_VALUE;}}
    public static String dateLabel(String d){long n=days(d);return n==Long.MAX_VALUE?"No date set":n<0?(-n)+"d past date":n==0?"Due today":n==1?"Due tomorrow":"In "+n+" days";}
    public static String number(double n){return n==Math.rint(n)?String.format(Locale.ROOT,"%.0f",n):String.format(Locale.ROOT,"%.3f",n).replaceAll("0+$","").replaceAll("\\.$","");}
    public static void validate(String type,JSONObject o){
        if(!Arrays.asList("items","recipes","shopping").contains(type))throw new IllegalArgumentException("Unknown record type.");
        try{UUID.fromString(o.optString("id"));}catch(Exception e){throw new IllegalArgumentException("Invalid record identifier.");}
        String title=o.optString(type.equals("recipes")?"title":"name").trim();if(title.isEmpty()||title.length()>160)throw new IllegalArgumentException("Add a name of up to 160 characters.");
        String[] dates=type.equals("recipes")?new String[]{"triedOn"}:new String[]{"expiry","opened","purchased","warranty"};for(String key:dates)if(!dateValid(o.optString(key)))throw new IllegalArgumentException("Use a valid YYYY-MM-DD date for "+key+".");
        if(!safeUrl(o.optString("sourceUrl")))throw new IllegalArgumentException("Use an http or https source link.");
        String field=type.equals("recipes")?"servings":"quantity";double n=o.optDouble(field,Double.NaN);if(!Double.isFinite(n)||n<0||n>1000000||(!type.equals("items")&&n==0))throw new IllegalArgumentException("Check the "+field+".");
        if(type.equals("items")){if(!Arrays.asList("grocery","equipment").contains(o.optString("kind")))throw new IllegalArgumentException("Choose groceries or equipment.");double min=o.optDouble("minimum",0);if(!Double.isFinite(min)||min<0||min>1000000)throw new IllegalArgumentException("Check the low-stock level.");}
        if(!type.equals("recipes")&&(o.optString("unit").isEmpty()||o.optString("location").trim().isEmpty()))throw new IllegalArgumentException("Choose a unit and storage place.");
        if(type.equals("recipes")&&(o.optInt("rating")<0||o.optInt("rating")>5||o.optInt("minutes")<0||o.optInt("minutes")>100000))throw new IllegalArgumentException("Check the recipe time and rating.");
        if(o.toString().length()>100000)throw new IllegalArgumentException("This entry is too large.");
    }
    public static boolean safeUrl(String s){if(s.isEmpty())return true;try{java.net.URI u=new java.net.URI(s);return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}}
    private void write(String type,JSONObject o){ContentValues v=new ContentValues();v.put("id",o.optString("id"));v.put("type",type);v.put("body",o.toString());v.put("updated",System.currentTimeMillis());getWritableDatabase().insertOrThrow("records",null,v);}
    private void replace(String type,JSONObject o){getWritableDatabase().delete("records","id=? AND type=?",new String[]{o.optString("id"),type});write(type,o);}
    public synchronized JSONObject update(String type,JSONObject input){getWritableDatabase().beginTransaction();try{JSONObject o=copy(input);validate(type,o);JSONObject old=get(o.optString("id"),type);if(old==null){if(o.optInt("revision")!=0)throw new IllegalArgumentException("This entry was removed.");}else if(old.optInt("revision")!=o.optInt("revision"))throw new IllegalArgumentException("This entry changed. Reopen it and try again.");put(o,"revision",o.optInt("revision")+1);put(o,"updated",java.time.Instant.now().toString());o.remove("owner");replace(type,o);getWritableDatabase().setTransactionSuccessful();return o;}finally{getWritableDatabase().endTransaction();}}
    public synchronized void remove(String type,JSONObject o){JSONObject current=get(o.optString("id"),type);if(current==null)return;if(current.optInt("revision")!=o.optInt("revision"))throw new IllegalArgumentException("This entry changed. Reopen it first.");getWritableDatabase().delete("records","id=? AND type=?",new String[]{o.optString("id"),type});}
    public synchronized List<String> places(){LinkedHashSet<String> out=new LinkedHashSet<>(Arrays.asList(DEFAULT_PLACES));try{JSONArray a=new JSONArray(meta("places","[]"));for(int i=0;i<a.length();i++)out.add(a.optString(i));}catch(JSONException ignored){}return new ArrayList<>(out);}
    public synchronized void addPlace(String name){name=name.trim();if(name.isEmpty()||name.length()>60)throw new IllegalArgumentException("Use a place name of 1–60 characters.");List<String> p=places();for(String s:p)if(s.equalsIgnoreCase(name))return;p.add(name);metaPut("places",new JSONArray(p).toString());}
    private String meta(String key,String def){try(Cursor c=getReadableDatabase().query("meta",new String[]{"value"},"key=?",new String[]{key},null,null,null)){return c.moveToFirst()?c.getString(0):def;}}
    private void metaPut(String key,String value){ContentValues v=new ContentValues();v.put("key",key);v.put("value",value);getWritableDatabase().insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public synchronized int restock(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{int count=0;for(JSONObject s:records("shopping")){if(s.optInt("checked")==0)continue;JSONObject linked=get(s.optString("itemId"),"items");if(linked!=null&&linked.optString("kind").equals("grocery")&&linked.optString("unit").equals(s.optString("unit"))&&linked.optString("expiry").isEmpty()&&linked.optString("opened").isEmpty()){put(linked,"quantity",linked.optDouble("quantity")+s.optDouble("quantity"));update("items",linked);}else{JSONObject item=blankItem("grocery");for(String k:new String[]{"name","unit","quantity","location","category"})put(item,k,s.opt(k));update("items",item);}remove("shopping",s);count++;}db.setTransactionSuccessful();return count;}finally{db.endTransaction();}}
    public synchronized JSONObject exportData(){return j("app","Kitchenkeeper","formatVersion",2,"exportedAt",java.time.Instant.now().toString(),"items",new JSONArray(records("items")),"recipes",new JSONArray(records("recipes")),"shopping",new JSONArray(records("shopping")),"locations",new JSONArray(places()));}
    public synchronized String importData(JSONObject backup,Set<String> photoIds){if(!backup.has("items")||!backup.has("recipes")||!backup.has("shopping"))throw new IllegalArgumentException("Choose a Kitchenkeeper JSON export.");int added=0,skipped=0,missing=0;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{for(String type:new String[]{"items","recipes","shopping"}){JSONArray a=backup.optJSONArray(type);if(a==null||a.length()>10000)throw new IllegalArgumentException("Invalid or oversized backup.");for(int i=0;i<a.length();i++){JSONObject o=copy(a.getJSONObject(i));JSONObject base=type.equals("recipes")?blankRecipe():type.equals("shopping")?blankShopping():blankItem(o.optString("kind","grocery"));for(Iterator<String> it=base.keys();it.hasNext();){String k=it.next();if(o.has(k))put(base,k,o.get(k));}validate(type,base);if(get(base.optString("id"),type)!=null){skipped++;continue;}String photo=base.optString("photo");if(!photo.isEmpty()&&!photoIds.contains(photo)){put(base,"photo","");missing++;}put(base,"revision",1);write(type,base);added++;}}
      JSONArray locations=backup.optJSONArray("locations");if(locations!=null)for(int i=0;i<Math.min(100,locations.length());i++)addPlace(locations.optString(i));db.setTransactionSuccessful();return added+" entries imported; "+skipped+" existing entries kept."+(missing>0?" "+missing+" photo references need new photos.":"");
    }catch(JSONException e){throw new IllegalArgumentException("The backup contains invalid records.");}finally{db.endTransaction();}}
}
