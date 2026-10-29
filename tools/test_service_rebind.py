"""Compile the real PreconditioningService against small Android fixtures and check
bind -> unbind -> return during a session; no car access."""
from pathlib import Path
import subprocess, json, os, shutil
ROOT = Path(__file__).resolve().parents[1]
def java_tool(name):
    executable = name + ('.exe' if os.name == 'nt' else '')
    home = os.environ.get('JAVA_HOME')
    if home:
        candidate = Path(home) / 'bin' / executable
        if candidate.is_file():
            return str(candidate)
    found = shutil.which(executable)
    if found:
        return found
    raise SystemExit('JDK required: set JAVA_HOME or put java and javac on PATH')

JAVAC, JAVA = java_tool('javac'), java_tool('java')
work = ROOT/'build/service-rebind'
if work.exists():
    shutil.rmtree(work)
# Handlers share one fake clock; tests drain due work explicitly, so ordering is deterministic.
stubs = {
 'android/os/IBinder': 'public interface IBinder {}',
 'android/os/Binder': 'public class Binder implements IBinder {}',
 'android/os/Looper': 'public class Looper {}',
 'android/os/SystemClock': '''public class SystemClock { public static long now=1000;
 public static long elapsedRealtime(){return now;} }''',
 'android/os/HandlerThread': '''public class HandlerThread { public HandlerThread(String n){}
 public void start(){} public Looper getLooper(){return new Looper();} public boolean quitSafely(){return true;} }''',
 'android/os/Handler': '''import java.util.*;
public class Handler { static final List<Handler> all=new ArrayList<>(); final List<Object[]> queue=new ArrayList<>();
 public Handler(Looper l){all.add(this);}
 public boolean post(Runnable r){return postDelayed(r,0);}
 public boolean postDelayed(Runnable r,long d){queue.add(new Object[]{r,SystemClock.now+d});return true;}
 public void removeCallbacks(Runnable r){queue.removeIf(e->e[0]==r);}
 public void removeCallbacksAndMessages(Object o){queue.clear();}
 public static void drain(){for(boolean ran=true;ran;){ran=false;for(Handler h:new ArrayList<>(all))
  for(Object[] e:new ArrayList<>(h.queue))if((Long)e[1]<=SystemClock.now&&h.queue.remove(e)){((Runnable)e[0]).run();ran=true;}}}
 public static void advance(long ms){for(long t=0;t<ms;t+=500){SystemClock.now+=500;drain();}} }''',
 'android/content/SharedPreferences': '''public interface SharedPreferences { boolean getBoolean(String k,boolean d); Editor edit();
 interface Editor { Editor putBoolean(String k,boolean v); boolean commit(); } }''',
 'android/content/Context': '''import java.util.*;
public abstract class Context { public static final int MODE_PRIVATE=0; final Map<String,Boolean> prefs=new HashMap<>();
 public SharedPreferences getSharedPreferences(String n,int m){return new SharedPreferences(){
  public boolean getBoolean(String k,boolean d){return prefs.getOrDefault(k,d);}
  public Editor edit(){return new Editor(){public Editor putBoolean(String k,boolean v){prefs.put(k,v);return this;}public boolean commit(){return true;}};}};} }''',
 'android/content/Intent': '''public class Intent { String action; public Intent(){} public Intent(Context c,Class<?> k){}
 public Intent setAction(String a){action=a;return this;} public String getAction(){return action;} }''',
 'android/app/PendingIntent': '''import android.content.*;
public class PendingIntent { public static final int FLAG_UPDATE_CURRENT=0;
 public static PendingIntent getActivity(Context c,int r,Intent i,int f){return new PendingIntent();}
 public static PendingIntent getService(Context c,int r,Intent i,int f){return new PendingIntent();} }''',
 'android/app/Notification': '''import android.content.*;
public class Notification { public static class Builder { public Builder(Context c){}
 public Builder setSmallIcon(int i){return this;} public Builder setContentTitle(CharSequence s){return this;}
 public Builder setContentText(CharSequence s){return this;} public Builder setOngoing(boolean b){return this;}
 public Builder setContentIntent(PendingIntent p){return this;} public Builder addAction(int i,CharSequence s,PendingIntent p){return this;}
 public Notification build(){return new Notification();} } }''',
 'android/app/Service': '''import android.content.*; import android.os.*;
public abstract class Service extends Context { public static final int START_NOT_STICKY=2; public int stops;
 public void onCreate(){} public void onDestroy(){} public abstract IBinder onBind(Intent i);
 public boolean onUnbind(Intent i){return false;} public void onRebind(Intent i){}
 public int onStartCommand(Intent i,int f,int s){return START_NOT_STICKY;}
 public final void stopSelf(){stops++;} public final void startForeground(int id,Notification n){}
 public final void stopForeground(boolean b){} }''',
 'com/paytondev/launchpad/R': 'final class R { static final class drawable { static final int ic_preconditioning=1; } }',
 'com/paytondev/launchpad/MainActivity': 'public class MainActivity {}',
 'com/paytondev/launchpad/VehicleClientFactory': '''final class VehicleClientFactory { static FakeVehicle current;
 static VehiclePort create(){return current;} }''',
 'com/paytondev/launchpad/FakeVehicle': '''final class FakeVehicle implements VehiclePort { int temperatureReads, starts;
 public String unavailableReason(){return "";}
 public Reading read(){return new Reading(2,0,1,-1);}
 public Integer readMinimumBatteryTemperatureC(){temperatureReads++;return 15;}
 public void beginManual(){starts++;} public void maintainManual(){} public void stopAndRelease(){}
 public long maintenanceIntervalMs(){return 5000;} }''',
}
for name, body in stubs.items():
    path = work/'src'/f'{name}.java'
    path.parent.mkdir(parents=True, exist_ok=True)
    package = name.rsplit('/', 1)[0].replace('/', '.')
    path.write_text(f'package {package};\n' + body)
test = work/'src/com/paytondev/launchpad/ServiceRebindTest.java'
test.write_text('''package com.paytondev.launchpad;
import android.content.Intent; import android.os.*;
import java.lang.reflect.Field;
public class ServiceRebindTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static boolean bound(PreconditioningService s)throws Exception{return (Boolean)field(s,"bound");}
 static boolean requested(PreconditioningService s)throws Exception{return ((PreconditioningSnapshot)field(s,"snapshot")).requested;}
 // Android rule: a client returning to a live service gets onRebind only if the last onUnbind returned true.
 static boolean rebindWanted;
 static void unbind(PreconditioningService s){rebindWanted=s.onUnbind(new Intent());Handler.drain();}
 static void returnToApp(PreconditioningService s){if(rebindWanted)s.onRebind(new Intent());Handler.drain();}
 static PreconditioningService create(FakeVehicle car){VehicleClientFactory.current=car;
  PreconditioningService s=new PreconditioningService();s.onCreate();Handler.drain();
  s.onBind(new Intent());Handler.drain();return s;}
 public static void main(String[] a)throws Exception{int passed=0;
  FakeVehicle car=new FakeVehicle(); PreconditioningService s=create(car);
  check(bound(s)&&car.temperatureReads==1,"first bind polls temperature");
  Handler.advance(10_500); check(car.temperatureReads==2,"poll repeats about every ten seconds while bound");
  s.onStartCommand(new Intent().setAction(PreconditioningService.START),0,1);Handler.drain();
  check(car.starts==1&&requested(s),"session requested");passed++;
  int beforeLeave=car.temperatureReads; unbind(s);
  check(!bound(s)&&s.stops==0,"service stays alive for the session after unbind");
  Handler.advance(35_000); check(car.temperatureReads==beforeLeave,"no display polling while away");passed++;
  returnToApp(s);
  check(bound(s),"returning to Launchpad during the session rebinds");
  check(car.temperatureReads==beforeLeave+1,"returning to Launchpad polls immediately");
  Handler.advance(10_500); check(car.temperatureReads==beforeLeave+2,"poll is rescheduled after rebind");
  check(requested(s),"rebind leaves the session requested");passed++;
  FakeVehicle idle=new FakeVehicle(); PreconditioningService t=create(idle); unbind(t);
  check(t.stops==1&&!bound(t),"idle service still stops after unbind");passed++;
  System.out.println(passed+" service rebind scenarios passed (host fixtures, no car access)");
 }
}''')
production = ROOT/'app/src/main/java/com/paytondev/launchpad'
names = ('PreconditioningService', 'PreconditioningController', 'PreconditioningSnapshot', 'VehiclePort',
         'BatteryTemperature', 'BatteryTemperatureReading', 'TemperatureHistory')
sources = [str(p) for p in (work/'src').rglob('*.java')] + [str(production/f'{n}.java') for n in names]
classes = work/'classes'; classes.mkdir()
subprocess.run([JAVAC, '-d', str(classes), *sources], check=True, timeout=60)
result = subprocess.run([JAVA, '-cp', str(classes), 'com.paytondev.launchpad.ServiceRebindTest'],
                        check=True, capture_output=True, text=True, timeout=15)
(work/'results.json').write_text(json.dumps({'passed': True, 'scenarios': 4,
    'scope': 'host Android fixtures, no car access', 'output': result.stdout}, indent=2) + '\n')
print(result.stdout)
