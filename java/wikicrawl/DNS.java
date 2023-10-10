package wikicrawl;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DNS {

    public static DohResolver dohResolver = new DohResolver( "https://cloudflare-dns.com/dns-query");

    public interface INameService extends InvocationHandler {
        public static void install(final INameService dns) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
            final Class<?> inetAddressClass = InetAddress.class;
            Object neu;
            Field nameServiceField;
            try {
                final Class<?> iface = Class.forName("java.net.InetAddress$NameService");
                nameServiceField = inetAddressClass.getDeclaredField("nameService");
                neu = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, dns);
            } catch(final ClassNotFoundException|NoSuchFieldException e) {
                nameServiceField = inetAddressClass.getDeclaredField("nameServices");
                final Class<?> iface = Class.forName("sun.net.spi.nameservice.NameService");
                neu = Arrays.asList(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, dns));
            }
            nameServiceField.setAccessible(true);
            nameServiceField.set(inetAddressClass, neu);
        }

        /**
         * Lookup a host mapping by name. Retrieve the IP addresses associated with a host
         *
         * @param host the specified hostname
         * @return array of IP addresses for the requested host
         * @throws UnknownHostException  if no IP address for the {@code host} could be found
         */
        InetAddress[] lookupAllHostAddr(final String host) throws UnknownHostException;

        /**
         * Lookup the host corresponding to the IP address provided
         *
         * @param addr byte array representing an IP address
         * @return {@code String} representing the host name mapping
         * @throws UnknownHostException
         *             if no host found for the specified IP address
         */
        String getHostByAddr(final byte[] addr) throws UnknownHostException;

        @Override default public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            switch(method.getName()) {
                case "lookupAllHostAddr": return lookupAllHostAddr((String)args[0]);
                case "getHostByAddr"    : return getHostByAddr    ((byte[])args[0]);
                default                 :
                    final StringBuilder o = new StringBuilder();
                    o.append(method.getReturnType().getCanonicalName()+" "+method.getName()+"(");
                    final Class<?>[] ps = method.getParameterTypes();
                    for(int i=0;i<ps.length;++i) {
                        if(i>0) o.append(", ");
                        o.append(ps[i].getCanonicalName()).append(" p").append(i);
                    }
                    o.append(")");
                    throw new UnsupportedOperationException(o.toString());
            }
        }
    }

    INameService dns = new INameService() {
        @Override
        public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
            StringBuilder builder = new StringBuilder(host);
            builder.append('.');
            String query = builder.toString();

            Record queryTxtRecord = null;
            try {
                queryTxtRecord = Record.newRecord(new Name(query), Type.A, DClass.IN);
                Message queryMessage = Message.newQuery(queryTxtRecord);
                Message response = dohResolver.query(queryMessage, 5000);

                if (response.getRcode() == Rcode.NOERROR) {
                    List<InetAddress> addressList = new ArrayList<InetAddress>();
                    RRset[] answers = response.getSectionRRsets(Section.ANSWER).toArray(new RRset[0]);
                    for (RRset answer: answers) {
                        long ttl = answer.getTTL();
                        while(answer.size() != 0) {
                            Record record = answer.first();
                            answer.deleteRR(record);

                            String ip = record.rdataToString();
                            InetAddress inetAddress = InetAddress.getByName(ip);

                            addressList.add(inetAddress);
                        }
                    }

                    return addressList.toArray(new InetAddress[addressList.size()]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new InetAddress[0];
        }

        @Override
        public String getHostByAddr(byte[] addr) throws UnknownHostException {
            return null;
        }
    };

    {
        try {
            INameService.install(dns);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
