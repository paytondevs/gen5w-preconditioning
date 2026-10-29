"""Compile the real reader against small Binder contract fixtures; no car access."""
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
work = ROOT/'build/temperature-contract'
(work/'android/os').mkdir(parents=True, exist_ok=True)
stubs = {
 'RemoteException': 'public class RemoteException extends Exception { public RemoteException(String s){super(s);} }',
 'IBinder': '''public interface IBinder { int INTERFACE_TRANSACTION=1598968902;
 boolean transact(int c,Parcel d,Parcel r,int f)throws RemoteException;
 String getInterfaceDescriptor()throws RemoteException; boolean isBinderAlive(); }''',
 'Binder': '''public class Binder implements IBinder {
 public void attachInterface(Object o,String s){} public String getInterfaceDescriptor(){return "";}
 public boolean isBinderAlive(){return true;}
 public boolean transact(int c,Parcel d,Parcel r,int f)throws RemoteException{return onTransact(c,d,r,f);}
 protected boolean onTransact(int c,Parcel d,Parcel r,int f)throws RemoteException{return false;} }''',
 'ServiceManager': '''public class ServiceManager { public static IBinder current;
 public static IBinder getService(String n){if(!n.equals("com.hkmc.micom.cluster"))throw new AssertionError(n);return current;} }''',
 'Parcel': '''public class Parcel { public String token;public int[] array;public IBinder binder;public boolean ok;public int extra;
 public static Parcel obtain(){return new Parcel();} public void recycle(){}
 public void writeInterfaceToken(String s){token=s;} public void enforceInterface(String s){if(!s.equals(token))throw new SecurityException();}
 public void writeStrongBinder(IBinder b){binder=b;}public void writeString(String s){}
 public int[] createIntArray(){return array;}public int dataAvail(){return extra;}
 public void writeNoException(){ok=true;}public void readException(){if(!ok)throw new SecurityException("remote failure");} }''',
}
for name, body in stubs.items():
    (work/'android/os'/f'{name}.java').write_text('package android.os;\n'+body)
test = work/'com/paytondev/launchpad/TemperatureBinderTest.java'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text('''package com.paytondev.launchpad;
import android.os.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
public class TemperatureBinderTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static final String TOKEN="com.hkmc.cluster.INavigationClusterListener";
 static class Remote extends Binder {
  List<Integer> calls=new ArrayList<>(); IBinder listener; int mode;
  CountDownLatch release=new CountDownLatch(1),finished=new CountDownLatch(1),entered=new CountDownLatch(1);
  Remote(int mode){this.mode=mode;}
  public String getInterfaceDescriptor(){return mode==6?"wrong":"com.hkmc.cluster.IClusterService";}
  public boolean isBinderAlive(){return mode!=7;}
  void response(int temp,boolean malformed)throws RemoteException{
   Parcel d=Parcel.obtain(),r=Parcel.obtain();d.token=TOKEN;
   d.array=malformed?new int[7]:new int[]{96,temp,16620,0,113,0,202,0};
   check(listener.transact(16,d,r,0));check(r.ok);
  }
  public boolean transact(int code,Parcel d,Parcel r,int flags)throws RemoteException{
   check(flags==0 && d.token.equals("com.hkmc.cluster.IClusterService")); calls.add(code);
   check(code==37||code==38||code==109);
   if(code==37){listener=d.binder;check(listener!=null);
    if(mode==3)throw new RemoteException("registration failed");
    response(60,false); // Must ignore unsolicited callback before explicit request.
   }
   if(code==109){check(d.binder==null);
    if(mode==4)throw new RemoteException("request failed");
    if(mode==8){entered.countDown();try{release.await();}catch(InterruptedException e){throw new RuntimeException(e);}}
    if(mode!=2)response(20,mode==1);
   }
   if(code==38){check(d.binder==listener);finished.countDown();if(mode==5)throw new RemoteException("remove failed");}
   r.writeNoException(); return true;
  }
 }
 public static void main(String[] args)throws Exception {
  int passed=0;
  for(int mode=0;mode<8;mode++){
   Remote remote=new Remote(mode);ServiceManager.current=remote;
   Integer value=ClusterBatteryTemperatureReader.read();
   check(mode==0?Integer.valueOf(20).equals(value):value==null);
   if(mode>=6)check(remote.calls.isEmpty());
   else check(remote.calls.equals(mode==3?Arrays.asList(37,38):Arrays.asList(37,109,38)));
   passed++;
  }
  Remote slow=new Remote(8);ServiceManager.current=slow;
  long before=System.nanoTime();check(ClusterBatteryTemperatureReader.read()==null);
  long ms=(System.nanoTime()-before)/1000000;check(ms<2600);
  check(ClusterBatteryTemperatureReader.read()==null); // No second hung helper.
  check(slow.calls.equals(Arrays.asList(37,109)));
  slow.release.countDown();slow.finished.await();Thread.sleep(50);
  Remote next=new Remote(1);ServiceManager.current=next;
  check(ClusterBatteryTemperatureReader.read()==null); // Never reuse late20C from old request.
  check(next.calls.equals(Arrays.asList(37,109,38)));passed++;
  Remote shared=new Remote(8);ServiceManager.current=shared;
  Integer[] values=new Integer[2];
  Thread first=new Thread(()->values[0]=ClusterBatteryTemperatureReader.read());
  Thread second=new Thread(()->values[1]=ClusterBatteryTemperatureReader.read());
  first.start();shared.entered.await();second.start();
  long deadline=System.nanoTime()+500000000L;
  while(second.getState()!=Thread.State.TIMED_WAITING && System.nanoTime()<deadline)Thread.sleep(1);
  check(second.getState()==Thread.State.TIMED_WAITING);
  shared.release.countDown();first.join(1000);second.join(1000);
  check(!first.isAlive()&&!second.isAlive());
  check(Integer.valueOf(20).equals(values[0])&&Integer.valueOf(20).equals(values[1]));
  check(shared.calls.equals(Arrays.asList(37,109,38)));passed++;
  System.out.println(passed+" Binder contract scenarios passed (host fixtures, no car access)");
 }
}''')
production = ROOT/'app/src/main/java/com/paytondev/launchpad'
sources = [str(p) for p in work.rglob('*.java')]+[str(production/n) for n in ('BatteryTemperature.java','ClusterBatteryTemperatureReader.java')]
classes=work/'classes';classes.mkdir(exist_ok=True)
subprocess.run([JAVAC,'-d',str(classes),*sources],check=True,timeout=60)
result=subprocess.run([JAVA,'-cp',str(classes),'com.paytondev.launchpad.TemperatureBinderTest'],check=True,capture_output=True,text=True,timeout=15)
(work/'results.json').write_text(json.dumps({'passed':True,'scenarios':10,'scope':'host Binder fixtures, no car access','output':result.stdout},indent=2)+'\n')
print(result.stdout)
