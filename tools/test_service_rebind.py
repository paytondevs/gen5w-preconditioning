"""Compile the real PreconditioningService against small Android fixtures and check
bind -> unbind -> return during a session and the preconditioning scheduler; no car access."""
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
# WallClock is a fixed offset from that clock, so advancing one advances both. Preferences,
# the alarm and the notification manager are static: they survive a simulated process kill.
stubs = {
 'android/os/IBinder': 'public interface IBinder {}',
 'android/os/Binder': 'public class Binder implements IBinder {}',
 'android/os/Looper': 'public class Looper {}',
 'android/os/SystemClock': """public class SystemClock { public static long now=1000;
 public static long elapsedRealtime(){return now;} }""",
 'android/os/HandlerThread': """public class HandlerThread { public HandlerThread(String n){}
 public void start(){} public Looper getLooper(){return new Looper();} public boolean quitSafely(){return true;} }""",
 'android/os/Handler': """import java.util.*;
public class Handler { public static final List<Handler> all=new ArrayList<>(); final List<Object[]> queue=new ArrayList<>();
 public Handler(Looper l){all.add(this);}
 public boolean post(Runnable r){return postDelayed(r,0);}
 public boolean postDelayed(Runnable r,long d){queue.add(new Object[]{r,SystemClock.now+d});return true;}
 public void removeCallbacks(Runnable r){queue.removeIf(e->e[0]==r);}
 public void removeCallbacksAndMessages(Object o){queue.clear();}
 public static void drain(){for(boolean ran=true;ran;){ran=false;for(Handler h:new ArrayList<>(all))
  for(Object[] e:new ArrayList<>(h.queue))if((Long)e[1]<=SystemClock.now&&h.queue.remove(e)){((Runnable)e[0]).run();ran=true;}}}
 public static void advance(long ms){for(long t=0;t<ms;t+=500){SystemClock.now+=500;drain();}} }""",
 'android/content/SharedPreferences': """public interface SharedPreferences { boolean getBoolean(String k,boolean d); String getString(String k,String d); Editor edit();
 interface Editor { Editor putBoolean(String k,boolean v); Editor putString(String k,String v); Editor remove(String k); boolean commit(); } }""",
 'android/content/Context': """import java.util.*;
public abstract class Context { public static final int MODE_PRIVATE=0;
 public static final String ALARM_SERVICE="alarm", NOTIFICATION_SERVICE="notification";
 public static final Map<String,Object> prefs=new HashMap<>();
 public Object getSystemService(String n){return ALARM_SERVICE.equals(n)?android.app.AlarmManager.instance
  :NOTIFICATION_SERVICE.equals(n)?android.app.NotificationManager.instance:null;}
 public SharedPreferences getSharedPreferences(String n,int m){return new SharedPreferences(){
  public boolean getBoolean(String k,boolean d){Object v=prefs.get(k);return v==null?d:(Boolean)v;}
  public String getString(String k,String d){Object v=prefs.get(k);return v==null?d:(String)v;}
  public Editor edit(){return new Editor(){final Map<String,Object> pending=new HashMap<>();final Set<String> gone=new HashSet<>();
   public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
   public Editor putString(String k,String v){pending.put(k,v);return this;}
   public Editor remove(String k){gone.add(k);return this;}
   public boolean commit(){for(String k:gone)prefs.remove(k);prefs.putAll(pending);return true;}};}};} }""",
 'android/content/Intent': """import java.util.*;
public class Intent { String action; final Map<String,Object> extras=new HashMap<>(); public Intent(){} public Intent(Context c,Class<?> k){}
 public Intent setAction(String a){action=a;return this;} public String getAction(){return action;}
 public Intent putExtra(String k,String v){extras.put(k,v);return this;} public Intent putExtra(String k,long v){extras.put(k,v);return this;}
 public String getStringExtra(String k){return (String)extras.get(k);}
 public long getLongExtra(String k,long d){Object v=extras.get(k);return v==null?d:(Long)v;} }""",
 'android/text/format/DateFormat': """public class DateFormat { public static boolean is24HourFormat(android.content.Context c){return false;} }""",
 'android/app/PendingIntent': """import android.content.*;
public class PendingIntent { public static final int FLAG_UPDATE_CURRENT=0;
 public static PendingIntent getActivity(Context c,int r,Intent i,int f){return new PendingIntent();}
 public static PendingIntent getService(Context c,int r,Intent i,int f){return new PendingIntent();} }""",
 'android/app/AlarmManager': """public class AlarmManager { public static final int RTC_WAKEUP=0; public static AlarmManager instance=new AlarmManager();
 public long nextAt=-1; public int sets, cancels;
 public void setExact(int type,long at,PendingIntent p){nextAt=at;sets++;}
 public void cancel(PendingIntent p){nextAt=-1;cancels++;} }""",
 'android/app/NotificationManager': """public class NotificationManager { public static NotificationManager instance=new NotificationManager();
 public int posted; public void notify(int id,Notification n){posted++;} }""",
 'android/app/Notification': """import android.content.*;
public class Notification { public static class Builder { public Builder(Context c){}
 public Builder setSmallIcon(int i){return this;} public Builder setContentTitle(CharSequence s){return this;}
 public Builder setContentText(CharSequence s){return this;} public Builder setOngoing(boolean b){return this;}
 public Builder setAutoCancel(boolean b){return this;}
 public Builder setContentIntent(PendingIntent p){return this;} public Builder addAction(int i,CharSequence s,PendingIntent p){return this;}
 public Notification build(){return new Notification();} } }""",
 'android/app/Service': """import android.content.*; import android.os.*;
public abstract class Service extends Context { public static final int START_NOT_STICKY=2; public int stops; public boolean fg;
 public void onCreate(){} public void onDestroy(){} public abstract IBinder onBind(Intent i);
 public boolean onUnbind(Intent i){return false;} public void onRebind(Intent i){}
 public int onStartCommand(Intent i,int f,int s){return START_NOT_STICKY;}
 public final void stopSelf(){stops++;} public final void startForeground(int id,Notification n){fg=true;}
 public final void stopForeground(boolean b){fg=false;} }""",
 'com/paytondev/launchpad/R': 'final class R { static final class drawable { static final int ic_preconditioning=1; } }',
 'com/paytondev/launchpad/MainActivity': 'public class MainActivity {}',
 'com/paytondev/launchpad/WallClock': """public final class WallClock { public static long offset=1793613600000L;
 static long now(){return offset+android.os.SystemClock.now;} }""",
 'com/paytondev/launchpad/VehicleClientFactory': """final class VehicleClientFactory { static FakeVehicle current;
 static VehiclePort create(){return current;} }""",
 'com/paytondev/launchpad/FakeVehicle': """final class FakeVehicle implements VehiclePort { int polls, prechecks, starts, ready=1, minimum=15; Integer outside=1;
 public String unavailableReason(){return "";}
 public Reading read(){return new Reading(2,0,ready,-1);}
 public Integer readMinimumBatteryTemperatureC(){prechecks++;return minimum;}
 public BatteryTemperature.Sample readTemperatures(){polls++;return new BatteryTemperature.Sample(minimum,null,outside);}
 public void beginManual(){starts++;} public void maintainManual(){} public void stopAndRelease(){}
 public long maintenanceIntervalMs(){return 5000;} }""",
}
for name, body in stubs.items():
    path = work/'src'/f'{name}.java'
    path.parent.mkdir(parents=True, exist_ok=True)
    package = name.rsplit('/', 1)[0].replace('/', '.')
    path.write_text(f'package {package};\n' + body)
test = work/'src/com/paytondev/launchpad/ServiceRebindTest.java'
test.write_text("""package com.paytondev.launchpad;
import android.content.Context; import android.content.Intent; import android.os.*;
import android.app.AlarmManager; import android.app.NotificationManager;
import java.lang.reflect.Field;
public class ServiceRebindTest {
 static final long MIN=60_000L;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static boolean bound(PreconditioningService s)throws Exception{return (Boolean)field(s,"bound");}
 static boolean requested(PreconditioningService s)throws Exception{return ((PreconditioningSnapshot)field(s,"snapshot")).requested;}
 static PreconditioningSchedule schedule(PreconditioningService s)throws Exception{return (PreconditioningSchedule)field(s,"schedule");}
 static String kinds(PreconditioningService s)throws Exception{StringBuilder b=new StringBuilder();
  for(ScheduleLog.Event e:((ScheduleLog)field(s,"scheduleLog")).events){if(b.length()>0)b.append(',');b.append(e.kind);}return b.toString();}
 static boolean stored(){return Context.prefs.containsKey(PreconditioningService.SCHEDULE_KEY);}
 static long wall(){return WallClock.now();}
 // Android rule: a client returning to a live service gets onRebind only if the last onUnbind returned true.
 static boolean rebindWanted;
 static void unbind(PreconditioningService s){rebindWanted=s.onUnbind(new Intent());Handler.drain();}
 static void returnToApp(PreconditioningService s){if(rebindWanted)s.onRebind(new Intent());Handler.drain();}
 static void command(PreconditioningService s,Intent i){s.onStartCommand(i,0,1);Handler.drain();}
 static Intent action(String a){return new Intent().setAction(a);}
 static Intent arm(String mode,long target){return action(PreconditioningService.SCHEDULE_SET)
  .putExtra(PreconditioningService.EXTRA_MODE,mode).putExtra(PreconditioningService.EXTRA_TARGET,target);}
 static PreconditioningService create(FakeVehicle car){VehicleClientFactory.current=car;
  PreconditioningService s=new PreconditioningService();s.onCreate();Handler.drain();
  s.onBind(new Intent());Handler.drain();return s;}
 /** A process the system starts for an intent, with nothing bound. */
 static PreconditioningService started(FakeVehicle car,String a){VehicleClientFactory.current=car;
  PreconditioningService s=new PreconditioningService();s.onCreate();Handler.drain();command(s,action(a));return s;}
 /** Drop every handler: the process is gone; preferences, alarm and notifications remain. */
 static void kill(){Handler.all.clear();}
 static void reset(){kill();Context.prefs.clear();AlarmManager.instance=new AlarmManager();NotificationManager.instance=new NotificationManager();}
 public static void main(String[] a)throws Exception{int passed=0;
  FakeVehicle car=new FakeVehicle(); PreconditioningService s=create(car);
  check(bound(s)&&car.polls==1,"first bind polls temperature");
  Handler.advance(10_500); check(car.polls==2,"poll repeats about every ten seconds while bound");
  command(s,action(PreconditioningService.START));
  check(car.starts==1&&requested(s),"session requested");passed++;
  int beforeLeave=car.polls; unbind(s);
  check(!bound(s)&&s.stops==0,"service stays alive for the session after unbind");
  Handler.advance(35_000); check(car.polls==beforeLeave,"no display polling while away");passed++;
  returnToApp(s);
  check(bound(s),"returning to Launchpad during the session rebinds");
  check(car.polls==beforeLeave+1,"returning to Launchpad polls immediately");
  Handler.advance(10_500); check(car.polls==beforeLeave+2,"poll is rescheduled after rebind");
  check(requested(s),"rebind leaves the session requested");passed++;
  FakeVehicle idle=new FakeVehicle(); PreconditioningService t=create(idle); unbind(t);
  check(t.stops==1&&!bound(t),"idle service still stops after unbind");passed++;

  // Start-by fires at target - 70 s while Launchpad is closed.
  reset(); car=new FakeVehicle(); s=create(car);
  long target=wall()+10*MIN; command(s,arm("START_BY",target));
  PreconditioningSchedule armed=schedule(s);
  check(armed!=null&&armed.plannedSendAt==target-70_000,"start-by is armed 70 s before the target");
  check(AlarmManager.instance.nextAt==target-70_000,"the alarm backstop is set for the send time");
  check(stored()&&s.fg,"the schedule is stored and keeps a foreground notification");
  unbind(s); check(s.stops==0,"an armed schedule keeps the service alive after unbind");
  int polls=car.polls; Handler.advance(60_000);
  check(car.polls>=polls+5,"temperature keeps polling while armed and closed");
  Handler.advance(armed.plannedSendAt-wall()-1000); check(car.starts==0,"nothing is sent before the send time");
  Handler.advance(2000); check(car.starts==1&&requested(s),"Start is sent at the send time");
  check(schedule(s)==null&&!stored(),"the schedule is cleared once sent");
  check(kinds(s).equals("ARMED,FIRED"),"log: "+kinds(s));
  check(AlarmManager.instance.nextAt==-1&&NotificationManager.instance.posted==1,"alarm cancelled and outcome notified");passed++;

  // Cancelling while closed stops the service.
  reset(); car=new FakeVehicle(); s=create(car); command(s,arm("START_BY",wall()+10*MIN)); unbind(s);
  command(s,action(PreconditioningService.SCHEDULE_CANCEL));
  check(schedule(s)==null&&!stored()&&s.stops==1,"cancel clears the schedule and stops the idle service");
  check(AlarmManager.instance.nextAt==-1&&kinds(s).equals("ARMED,CANCELLED"),"cancel clears the alarm: "+kinds(s));
  check(car.starts==0,"cancel sends nothing");passed++;

  // Process killed while armed: the alarm brings it back and it sends.
  reset(); car=new FakeVehicle(); s=create(car); target=wall()+10*MIN;
  command(s,arm("START_BY",target)); unbind(s); kill();
  SystemClock.now+=(target-70_000)-wall()+30_000;
  FakeVehicle revived=new FakeVehicle(); PreconditioningService r=started(revived,PreconditioningService.SCHEDULE_FIRE);
  check(revived.starts==1&&requested(r),"a restored schedule inside its window sends Start");
  check(kinds(r).equals("ARMED,RESTORED,FIRED")&&!stored(),"log: "+kinds(r));passed++;

  // Restored after the grace window: missed, nothing sent.
  reset(); car=new FakeVehicle(); s=create(car); target=wall()+10*MIN;
  command(s,arm("START_BY",target)); unbind(s); kill();
  SystemClock.now+=target+PreconditioningSchedule.GRACE_MS+MIN-wall();
  revived=new FakeVehicle(); r=started(revived,PreconditioningService.SCHEDULE_RESTORE);
  check(revived.starts==0&&schedule(r)==null&&!stored(),"a schedule past its window is never sent late");
  check(kinds(r).endsWith("MISSED")&&r.stops==1&&NotificationManager.instance.posted==1,"missed is logged and notified: "+kinds(r));passed++;

  // Not READY at the send time: one deferral line, then sends once READY.
  reset(); car=new FakeVehicle(); s=create(car); target=wall()+5*MIN;
  command(s,arm("START_BY",target)); unbind(s); car.ready=0;
  Handler.advance(target-70_000-wall()+20_000);
  check(car.starts==0&&schedule(s)!=null&&stored(),"nothing sent while the car is not READY");
  check(kinds(s).equals("ARMED,DEFERRED"),"deferral logged once: "+kinds(s));
  car.ready=1; Handler.advance(2000);
  check(car.starts==1&&kinds(s).equals("ARMED,DEFERRED,FIRED"),"sends once READY: "+kinds(s));passed++;

  // Warm pack at the send time: skipped and notified, nothing sent.
  reset(); car=new FakeVehicle(); car.minimum=25; s=create(car); target=wall()+5*MIN;
  command(s,arm("START_BY",target)); unbind(s);
  Handler.advance(target-70_000-wall()+2000);
  check(car.starts==0&&schedule(s)==null&&!stored(),"a warm pack is skipped");
  check(kinds(s).equals("ARMED,SKIPPED")&&NotificationManager.instance.posted==1,"skip logged and notified: "+kinds(s));passed++;

  // A manual Start cancels the schedule first.
  reset(); car=new FakeVehicle(); s=create(car); command(s,arm("START_BY",wall()+30*MIN));
  command(s,action(PreconditioningService.START));
  check(car.starts==1&&schedule(s)==null&&kinds(s).equals("ARMED,CANCELLED"),"manual Start cancels: "+kinds(s));passed++;

  // Arrive-by re-plans as readings change and moves the alarm.
  reset(); car=new FakeVehicle(); car.minimum=5; car.outside=10; s=create(car); target=wall()+60*MIN;
  command(s,arm("ARRIVE_BY",target));
  check(schedule(s).plannedSendAt==target-30*MIN-70_000,"cold pack plans the capped 30 minutes");
  int sets=AlarmManager.instance.sets; car.minimum=15; Handler.advance(10_500);
  check(schedule(s).plannedSendAt==target-12*MIN-70_000,"warmer pack re-plans later: "+schedule(s).plannedSendAt);
  check(kinds(s).equals("ARMED,REPLANNED")&&AlarmManager.instance.sets==sets+1,"re-plan logged and alarm moved: "+kinds(s));
  check(PreconditioningSchedule.decode((String)Context.prefs.get(PreconditioningService.SCHEDULE_KEY)).plannedSendAt
   ==schedule(s).plannedSendAt,"the re-plan is stored");passed++;

  // A time too soon is rejected by the service, whatever the UI allowed.
  reset(); car=new FakeVehicle(); s=create(car); command(s,arm("START_BY",wall()+MIN));
  check(schedule(s)==null&&!stored()&&kinds(s).equals("REJECTED")&&AlarmManager.instance.sets==0,"too-soon target rejected");passed++;

  System.out.println(passed+" service scenarios passed (host fixtures, no car access)");
 }
}""")
production = ROOT/'app/src/main/java/com/paytondev/launchpad'
names = ('PreconditioningService', 'PreconditioningController', 'PreconditioningSnapshot', 'VehiclePort',
         'BatteryTemperature', 'BatteryTemperatureReading', 'TemperatureHistory', 'PreconditioningSchedule',
         'ArriveByPlanner', 'ScheduleLog', 'ScheduleFormat')
sources = [str(p) for p in (work/'src').rglob('*.java')] + [str(production/f'{n}.java') for n in names]
classes = work/'classes'; classes.mkdir()
subprocess.run([JAVAC, '-d', str(classes), *sources], check=True, timeout=60)
result = subprocess.run([JAVA, '-cp', str(classes), 'com.paytondev.launchpad.ServiceRebindTest'],
                        check=True, capture_output=True, text=True, timeout=15)
(work/'results.json').write_text(json.dumps({'passed': True, 'scenarios': 13,
    'scope': 'host Android fixtures, no car access', 'output': result.stdout}, indent=2) + '\n')
print(result.stdout)
