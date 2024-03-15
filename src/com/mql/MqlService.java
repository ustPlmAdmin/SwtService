package com.mql;

import com.dassault_systemes.platform.restServices.RestService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.client.fcs.FcsClient;
import com.matrixone.fcs.mcs.Checkout;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.TicketWrapper;
import matrix.util.MatrixException;

public class MqlService extends RestService {
    private static final Base64.Decoder bd = Base64.getDecoder();

    private static Map<String, String> dm;

    public static final Gson json = (new GsonBuilder()).setPrettyPrinting().create();

    public static long stopTime = 0L;

    public static final String delimiter = " = ";

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(dc("TU0vZGQveXl5eSBoaDptbTpzcyBhYQ=="));

    public static final SimpleDateFormat printDateFormat = new SimpleDateFormat(dc("ZGQuTU0ueXl5eSBISDptbQ=="));

    public String getKey(String line) {
        return gk(line);
    }

    public String getValue(String string) {
        return gv(string);
    }

    public String query(Context ctx, String query) throws FrameworkException {
        return q(ctx, query);
    }

    public List<Map<String, String>> select(Context ctx, String objectId, String... attrs) throws FrameworkException {
        return s(ctx, objectId, attrs);
    }

    public Map<String, String> row(Context ctx, String objectId, String... attrs) throws FrameworkException {
        return r(ctx, objectId, attrs);
    }

    public List<Map<String, String>> rows(Context ctx, String objectId, String... attrs) throws FrameworkException {
        return rs(ctx, objectId, attrs);
    }

    public List<String> list(Context ctx, String objectId, String attribute) throws FrameworkException {
        return l(ctx, objectId, attribute);
    }

    public List<String> lines(Context ctx, String objectId, String... attrs) throws FrameworkException {
        return ls(ctx, objectId, attrs);
    }

    public Map<String, Object> tree(Context ctx, String objectId, String... attrs) throws FrameworkException {
        return t(ctx, objectId, attrs);
    }

    public String scalar(Context ctx, String queryCode) throws FrameworkException {
        return sr(ctx, queryCode);
    }

    public String scalar(Context ctx, String objectId, String attribute) throws FrameworkException {
        return sr(ctx, objectId, attribute);
    }

    public List<String> findList(Context ctx, String type, String name, String attribute) throws FrameworkException {
        return fl(ctx, type, name, attribute);
    }

    public List<String> findListWhere(Context ctx, String type, String name, String where, String attribute) throws FrameworkException {
        return flw(ctx, type, name, where, attribute);
    }

    public List<String> findListWhereRevision(Context ctx, String type, String name, String revision, String where, String attribute) throws FrameworkException {
        return flwr(ctx, type, name, revision, where, attribute);
    }

    private List<String> deleteMultilines(String responseStr) {
        return dm(responseStr);
    }

    public Map<String, String> attrNames(String... attrs) {
        return an(attrs);
    }

    public List<Map<String, String>> findRows(Context ctx, String type, String name, String... attrs) throws FrameworkException {
        return fr(ctx, type, name, attrs);
    }

    public List<String> findRowsList(Context ctx, String type, String name, String attr) throws FrameworkException {
        return frl(ctx, type, name, attr);
    }

    public List<Map<String, String>> findRowsWhere(Context ctx, String type, String name, String where, String... attrs) throws FrameworkException {
        return frw(ctx, type, name, where, attrs);
    }

    public List<Map<String, String>> stringToRows(String response, Map<String, String> names) {
        return str(response, names);
    }

    public List<Map<String, String>> findRowsWhereRevision(Context ctx, String type, String name, String revision, String where, String... attrs) throws FrameworkException {
        return frwr(ctx, type, name, revision, where, attrs);
    }

    public List<String> findObjectsList(Context ctx, String type, String name, String attr) throws FrameworkException {
        return fol(ctx, type, name, attr);
    }

    public List<String> findObjectsListWhere(Context ctx, String type, String name, String where, String attr) throws FrameworkException {
        return folw(ctx, type, name, where, attr);
    }

    public Map<String, String> findObject(Context ctx, String type, String name, String... attrs) throws FrameworkException {
        return fo(ctx, type, name, attrs);
    }

    public List<Map<String, String>> findObjects(Context ctx, String type, String name, String... attrs) throws FrameworkException {
        return fos(ctx, type, name, attrs);
    }

    public String wrap(String param) {
        return w(param);
    }

    public List<Map<String, String>> findObjectsWhere(Context ctx, String type, String name, String where, String... attrs) throws FrameworkException {
        return fow(ctx, type, name, where, attrs);
    }

    public Map<String, String> conn(Context ctx, String pathid) throws FrameworkException {
        return c(ctx, pathid);
    }

    public List<String> paths(Context ctx, String name, String... types) throws FrameworkException {
        return p(ctx, name, types);
    }

    public List<String> paths(Context ctx, String name) throws FrameworkException {
        return p(ctx, name);
    }

    public Map<String, String> findAttributes(Context ctx, String name) throws FrameworkException {
        return fa(ctx, name);
    }

    public String getLastRevision(Context ctx, String type, String name) throws FrameworkException {
        return glr(ctx, type, name);
    }

    public String findScalar(Context ctx, String type, String name, String attribute) throws FrameworkException {
        return fs(ctx, type, name, attribute);
    }

    public String getRequestBody(HttpServletRequest request) throws IOException {
        return grb(request);
    }

    public Object getRequestObject(HttpServletRequest request, Type type) throws IOException {
        return gro(request, type);
    }

    public String getBaseUrl(HttpServletRequest request) {
        return gbu(request);
    }

    protected String getUrl(HttpServletRequest request, Context context, String oid, String fileName, String format) throws MatrixException {
        return gu(request, context, oid, fileName, format);
    }

    public Map<String, String> getDocs(HttpServletRequest request, Context ctx, String objectId) throws MatrixException, ParseException {
        return gd(request, ctx, objectId);
    }

    protected Context internalAuth(HttpServletRequest request) throws MatrixException {
        return ia(request);
    }

    protected Context internalAuth(HttpServletRequest request, String user) throws MatrixException {
        return ia(request, user);
    }

    protected Context internalAuth(String baseUrl, String user) throws MatrixException {
        return ia(baseUrl, user);
    }

    protected Context internalAuth(String baseUrl, String user, String role) throws MatrixException {
        return ia(baseUrl, user, role);
    }

    private static String dc(String a) {
        if (dm == null)
            dm = new HashMap<>();
        String b = dm.get(a);
        if (b == null) {
            b = new String(bd.decode(a));
            dm.put(a, b);
        }
        return b;
    }

    private String gk(String a) {
        int b = a.indexOf(" = ");
        return a.substring(0, b).trim();
    }

    private String gv(String a) {
        int b = a.indexOf(" = ");
        if (b == -1)
            return a;
        return a.substring(b + " = ".length());
    }

    private String q(Context a, String b) throws FrameworkException {
        long c = (new Date()).getTime();
        if (c < stopTime)
            return null;
        String d = MqlUtil.mqlCommand(a, b);
        Log.q(b, d);
        return d;
    }

    private List<Map<String, String>> s(Context a, String b, String... c) throws FrameworkException {
        if (b == null)
            return null;
        Map<String, String> d = an(c);
        List<String> f = ls(a, b, (String[]) d.keySet().toArray((Object[]) new String[d.size()]));
        List<Map<String, String>> g = new ArrayList<>();
        int h = f.size() / d.size();
        for (int i = 0; i < h; i++) {
            Map<String, String> k = new LinkedHashMap<>();
            for (int j = 0; j < d.size(); j++) {
                String l = f.get(j * h + i);
                String m = gk(l);
                String n = d.get(m);
                k.put((n == null) ? m : n, gv(l));
            }
            g.add(k);
        }
        return g;
    }

    private Map<String, String> r(Context a, String b, String... c) throws FrameworkException {
        List<Map<String, String>> d = s(a, b, c);
        return (d.size() > 0) ? s(a, b, c).get(0) : new LinkedHashMap<>();
    }

    private List<Map<String, String>> rs(Context a, String b, String... c) throws FrameworkException {
        if (b == null)
            throw new NullPointerException();
        Map<String, String> d = an(c);
        String e = q(a, dc("cHJpbnQgYnVzIA==") + b + dc("IHNlbGVjdCA=") + String.join(" ", d.keySet()) + ";");
        return str(e, d);
    }

    private List<String> l(Context a, String b, String c) throws FrameworkException {
        List<String> d = ls(a, b, new String[]{c});
        List<String> e = new ArrayList<>();
        for (String f : d) {
            if (f.contains(" = "))
                e.add(gv(f));
        }
        return e;
    }

    private List<String> ls(Context a, String b, String... c) throws FrameworkException {
        if (b == null || c == null)
            throw new NullPointerException();
        String d = q(a, dc("cHJpbnQgYnVzIA==") + b + dc("IHNlbGVjdCA=") + String.join(" ", (CharSequence[]) c) + ";");
        return dm(d);
    }

    private Map<String, Object> t(Context a, String b, String... c) throws FrameworkException {
        Map<String, String> d = an(c);
        List<String> e = ls(a, b, (String[]) d.keySet().toArray((Object[]) new String[d.size()]));
        Map<String, Object> f = new LinkedHashMap<>();
        for (String g : e) {
            String h = gk(g);
            String k = d.get(h);
            h = (k == null) ? h : k;
            String l = gv(g);
            Object m = f.get(h);
            if (m == null) {
                f.put(h, l);
                continue;
            }
            if (m instanceof String) {
                ArrayList<String> n = new ArrayList<>();
                n.add((String) m);
                n.add(l);
                f.put(h, n);
                continue;
            }
            if (m instanceof ArrayList)
                ((ArrayList<String>) m).add(l);
        }
        return f;
    }

    private String sr(Context a, String b) throws FrameworkException {
        String c = q(a, b);
        if (c.contains("="))
            return c.substring(c.indexOf("=") + 2);
        return "";
    }

    private String sr(Context a, String b, String c) throws FrameworkException {
        Map<String, String> d = r(a, b, new String[]{c});
        return (d.size() > 0) ? d.values().iterator().next() : null;
    }

    private List<String> fl(Context a, String b, String c, String d) throws FrameworkException {
        return flwr(a, b, c, glr(a, b, c), null, d);
    }

    private List<String> flw(Context a, String b, String c, String d, String i) throws FrameworkException {
        return flwr(a, b, c, glr(a, b, c), d, i);
    }

    private List<String> flwr(Context a, String b, String c, String d, String i, String f) throws FrameworkException {
        List<Map<String, String>> g = frwr(a, b, c, d, i, new String[]{f});
        List<String> h = new ArrayList<>();
        for (Map<String, String> k : g)
            h.add((new ArrayList<>(k.values())).get(0));
        return h;
    }

    private List<String> dm(String a) {
        List<String> b = new ArrayList<>();
        if (a != null && !a.isEmpty()) {
            String[] c = a.split("\n");
            for (String d : c) {
                if (!d.startsWith(dc("YnVzaW5lc3M=")) && !d.startsWith(dc("Y29ubmVjdGlvbg==")) && !d.contains(dc("ID0g"))) {
                    b.set(b.size() - 1, (String) b.get(b.size() - 1) + " " + d);
                } else {
                    b.add(d);
                }
            }
        }
        if (b.size() > 0)
            b.remove(0);
        return b;
    }

    private Map<String, String> an(String... a) {
        Map<String, String> b = new LinkedHashMap<>();
        for (int i = 0; i < a.length; i++) {
            String c = a[i];
            if (c.indexOf(':') != -1) {
                b.put(c.substring(0, c.indexOf(':')), c.substring(c.indexOf(':') + 1));
            } else {
                b.put(c, c);
            }
        }
        return b;
    }

    private List<Map<String, String>> fr(Context a, String b, String c, String... d) throws FrameworkException {
        return frwr(a, b, c, glr(a, b, c), null, d);
    }

    private List<String> frl(Context a, String b, String c, String d) throws FrameworkException {
        List<String> f = new ArrayList<>();
        for (Map<String, String> g : fr(a, b, c, new String[]{d}))
            f.add(g.get(d));
        return f;
    }

    private List<Map<String, String>> frw(Context a, String b, String c, String d, String... e) throws FrameworkException {
        return frwr(a, b, c, glr(a, b, c), d, e);
    }

    private List<Map<String, String>> str(String a, Map<String, String> b) {
        List<Map<String, String>> c = new ArrayList<>();
        List<String> d = dm(a);
        int e = d.size() / b.size();
        for (int i = 0; i < e; i++) {
            Map<String, String> f = new LinkedHashMap<>();
            for (int j = 0; j < b.size(); j++) {
                String g = d.get(j * e + i);
                String h = gk(g);
                String k = b.get(h);
                f.put((k == null) ? h : k, gv(g));
            }
            c.add(f);
        }
        return c;
    }

    private List<Map<String, String>> frwr(Context a, String b, String c, String d, String e, String... f) throws FrameworkException {
        b = w(b);
        c = w(c);
        d = w(d);
        Map<String, String> g = an(f);
        if (b.equals("*") && c.equals("*") && d.equals("*"))
            return null;
        String h = q(a, dc("dGVtcCBxdWVyeSBidXMg") + b + " " + c + " " + d + " " + ((e != null) ? (" where '" + e
                .replace('\'', '"') + "' ") : "") +
                dc("IHNlbGVjdCA=") + String.join(" ", g.keySet()) + ";");
        return str(h, g);
    }

    private List<String> fol(Context a, String b, String c, String d) throws FrameworkException {
        List<String> e = new ArrayList<>();
        for (Map<String, String> f : fos(a, b, c, new String[]{d}))
            e.add(f.get(d));
        return e;
    }

    private List<String> folw(Context a, String b, String c, String e, String f) throws FrameworkException {
        List<String> g = new ArrayList<>();
        for (Map<String, String> h : fow(a, b, c, e, new String[]{f}))
            g.add(h.get(f));
        return g;
    }

    private Map<String, String> fo(Context a, String b, String c, String... e) throws FrameworkException {
        List<Map<String, String>> f = fow(a, b, c, null, e);
        return (f.size() > 0) ? f.get(0) : new LinkedHashMap<>();
    }

    private List<Map<String, String>> fos(Context a, String b, String c, String... d) throws FrameworkException {
        return fow(a, b, c, null, d);
    }

    private String w(String a) {
        if (a == null)
            a = dc("Kg==");
        if (!a.equals(dc("Kg==")) && (!a.startsWith(dc("Ig==")) || !a.endsWith(dc("Ig=="))))
            a = dc("Ig==") + a + dc("Ig==");
        return a;
    }

    private List<Map<String, String>> fow(Context a, String b, String c, String d, String... e) throws FrameworkException {
        List<Map<String, String>> f = new ArrayList<>();
        b = w(b);
        c = w(c);
        if (b.equals(dc("Kg==")) && c.equals(dc("Kg==")))
            return null;
        Map<String, String> g = an(e);
        String h = q(a, dc("dGVtcCBxdWVyeSBidXMg") + b + " " + c + dc("ICog") + ((d != null) ? (
                dc("IHdoZXJlICc=") + d.replace('\'', '"') + "' ") : "") +
                dc("IHNlbGVjdCA=") + String.join(" ", g.keySet()) + ";");
        List<String> k = dm(h);
        Map<String, String> l = new LinkedHashMap<>();
        for (String m : k) {
            if (m.startsWith(dc("YnVzaW5lc3NvYmplY3Q="))) {
                f.add(l);
                l = new LinkedHashMap<>();
                continue;
            }
            String n = gk(m);
            String o = g.get(n);
            l.put((o == null) ? n : o, gv(m));
        }
        if (l.size() > 0)
            f.add(l);
        return f;
    }

    private Map<String, String> c(Context a, String b) throws FrameworkException {
        String c = sr(a, dc("cHJpbnQgY29ubmVjdGlvbiA=") + b + dc("IHNlbGVjdCB0by5uYW1l"));
        Map<String, String> d = new LinkedHashMap<>(fa(a, c));
        String e = q(a, dc("cHJpbnQgY29ubmVjdGlvbiA=") + b + dc("IHNlbGVjdCBhdHRyaWJ1dGVbKl0="));
        List<String> f = dm(e);
        for (String g : f)
            d.put(gk(g), gv(g));
        return d;
    }

    private List<String> p(Context a, String b, String... c) throws FrameworkException {
        List<String> d = new ArrayList<>();
        if (b == null)
            return d;
        List<Map<String, String>> e = fr(a, "*", b, new String[]{dc("ZnJvbVtWUExNcmVsL1BMTUNvbm5lY3Rpb24vVl9Pd25lcl0udG8ucGF0aHNbU2VtYW50aWNSZWxhdGlvbl0ucGF0aC5lbGVtZW50WzBdLnR5cGU6dHlwZQ=="),
                dc("ZnJvbVtWUExNcmVsL1BMTUNvbm5lY3Rpb24vVl9Pd25lcl0udG8ucGF0aHNbU2VtYW50aWNSZWxhdGlvbl0ucGF0aC5lbGVtZW50WzBdLnBoeXNpY2FsaWQ6cGh5c2ljYWxpZA=="),
                dc("ZnJvbVtWUExNcmVsL1BMTUNvbm5lY3Rpb24vVl9Pd25lcl0udG8ucGF0aHNbU2VtYW50aWNSZWxhdGlvbl0ucGF0aC5lbGVtZW50WzBdLmtpbmQ6a2luZA==")});
        if (c == null) {
            for (Map<String, String> f : e)
                d.add(f.get(dc("cGh5c2ljYWxpZA==")));
        } else {
            for (Map<String, String> g : e) {
                for (String h : c) {
                    if (((String) g.get(dc("dHlwZQ=="))).equals(h) && ((String) g.get(dc("a2luZA=="))).equals(dc("YnVzaW5lc3NvYmplY3Q=")))
                        d.add(g.get(dc("cGh5c2ljYWxpZA==")));
                }
            }
        }
        return d;
    }

    private List<String> p(Context a, String b) throws FrameworkException {
        return (b == null) ? new ArrayList<>() : p(a, b, null);
    }

    private Map<String, String> fa(Context a, String b) throws FrameworkException {
        Map<String, String> c = new LinkedHashMap<>();
        c.put(dc("bmFtZQ=="), b);
        b = w(b);
        if (b.equals("*"))
            return null;
        String d = q(a, dc("dGVtcCBxdWVyeSBidXMgKiA=") + b + " " + glr(a, dc("Kg=="), b) + dc("IHNlbGVjdCB0eXBlIHBoeXNpY2FsaWQgYXR0cmlidXRlWypdOw=="));
        List<String> e = dm(d);
        for (String f : e)
            c.put(gk(f), gv(f));
        return c;
    }

    private String glr(Context a, String b, String c) throws FrameworkException {
        List<Map<String, String>> d = fos(a, b, c, new String[]{dc("cmV2aXNpb24=")});
        return (d.size() == 0) ? null : (String) ((Map) d.get(d.size() - 1)).get(dc("cmV2aXNpb24="));
    }

    private String fs(Context a, String b, String c, String d) throws FrameworkException {
        List<String> e = fl(a, b, c, d);
        return (e.size() == 0) ? null : e.get(0);
    }

    private String grb(HttpServletRequest a) throws IOException {
        ServletInputStream servletInputStream = a.getInputStream();
        StringBuilder c = new StringBuilder();
        int d;
        while ((d = servletInputStream.read()) != -1)
            c.append((char) d);
        return c.toString();
    }

    private Object gro(HttpServletRequest a, Type b) throws IOException {
        return json.fromJson(grb(a), b);
    }

    private String gbu(HttpServletRequest a) {
        if (a.getServerName().equals(dc("MTAuMTAuNDcuMTA2")))
            return dc("aHR0cHM6Ly8zZHNwYWNlLW0wMDEuc3ctdGVjaC5ieTo0NDQ=");
        if (a.getServerName().equals(dc("MTAuMTAuMjUuMjAw")))
            return dc("aHR0cHM6Ly8zZHNwYWNlLXN0dWR5LnN3LXRlY2guYnk6NDQ0");
        if (a.getServerName().equals("localhost"))
            return dc("aHR0cHM6Ly8zZHNwYWNlLW0wMDEuc3ctdGVjaC5ieTo0NDQ=");

        return dc("aHR0cHM6Ly8zZHNwYWNlLnN3LXRlY2guYnk6NDQ0");
    }

    private String gu(HttpServletRequest a, Context b, String c, String d, String e) throws MatrixException {
        BusinessObject f = new BusinessObject(c);
        f.open(b);
        ArrayList<BusinessObjectProxy> g = new ArrayList<>();
        BusinessObjectProxy h = new BusinessObjectProxy(c, dc("Z2VuZXJpYw=="), d, false, false);
        g.add(h);
        TicketWrapper k = Checkout.doIt(b, gbu(a) + dc("L2ludGVybmFs"), g);
        return k.getActionURL() + "?" +
                FcsClient.resolveFcsParam(dc("am9iVGlja2V0")) + "=" + XSSUtil.encodeForURL(k.getExportString()) +
                dc("Jm5hbWU9") + XSSUtil.encodeForURL(d) +
                dc("JkNBVENhY2hlS2V5PQ==") + XSSUtil.encodeForURL(c + e + d);
    }

    private Map<String, String> gd(HttpServletRequest a, Context b, String c) throws MatrixException, ParseException {
        Map<String, String> d = new LinkedHashMap<>();
        List<Map<String, String>> e = s(b, c, new String[]{dc("ZnJvbVtWUE1SZXBJbnN0YW5jZV0udG8uZnJvbVtBUkNIUmVmZXJlbmNlT2JqZWN0XS50by5pZDppZA=="),
                dc("ZnJvbVtWUE1SZXBJbnN0YW5jZV0udG8uZnJvbVtBUkNIUmVmZXJlbmNlT2JqZWN0XS50by5mb3JtYXQ6Zm9ybWF0"),
                dc("ZnJvbVtWUE1SZXBJbnN0YW5jZV0udG8uZnJvbVtBUkNIUmVmZXJlbmNlT2JqZWN0XS50by5mcm9tW0xhdGVzdCBWZXJzaW9uXS50by5tb2RpZmllZDptb2RpZmllZA=="),
                dc("ZnJvbVtWUE1SZXBJbnN0YW5jZV0udG8uZnJvbVtBUkNIUmVmZXJlbmNlT2JqZWN0XS50by5mcm9tW0xhdGVzdCBWZXJzaW9uXS50by5hdHRyaWJ1dGVbVGl0bGVdOmZpbGVuYW1l")});
        List<Map<String, String>> f = s(b, c, new String[]{dc("ZnJvbVtWUExNcmVsL1BMTUNvbm5lY3Rpb24vVl9Pd25lcl0udG8ucGF0aHNbU2VtYW50aWNSZWxhdGlvbl0ucGF0aC5lbGVtZW50WzBdLnR5cGU6dHlwZQ=="),
                dc("ZnJvbVtWUExNcmVsL1BMTUNvbm5lY3Rpb24vVl9Pd25lcl0udG8ucGF0aHNbU2VtYW50aWNSZWxhdGlvbl0ucGF0aC5lbGVtZW50WzBdLnBoeXNpY2FsaWQ6cGh5c2ljYWxpZA==")});
        for (Map<String, String> g : f) {
            if (((String) g.get(dc("dHlwZQ=="))).equals(dc("RG9jdW1lbnQ=")) || ((String) g.get(dc("dHlwZQ=="))).equals(dc("QVJDSERvY3VtZW50")))
                try {
                    e.addAll(s(b, g.get(dc("cGh5c2ljYWxpZA==")), new String[]{dc("aWQ="),
                            dc("Zm9ybWF0"),
                            dc("ZnJvbVtMYXRlc3QgVmVyc2lvbl0udG8ubW9kaWZpZWQ6bW9kaWZpZWQ="),
                            dc("ZnJvbVtMYXRlc3QgVmVyc2lvbl0udG8uYXR0cmlidXRlW1RpdGxlXTpmaWxlbmFtZQ==")}));
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
        }
        Map<String, Map<String, String>> h = new LinkedHashMap<>();
        for (Map<String, String> k : e) {
            String l = k.get(dc("ZmlsZW5hbWU="));
            if (h.get(l) != null) {
                long m = dateFormat.parse((String) ((Map) h.get(l)).get(dc("bW9kaWZpZWQ="))).getTime();
                long n = dateFormat.parse(k.get(dc("bW9kaWZpZWQ="))).getTime();
                if (m < n)
                    h.put(l, k);
                continue;
            }
            h.put(l, k);
        }
        for (Map<String, String> o : h.values()) {
            String p = gu(a, b, o.get(dc("aWQ=")), o.get(dc("ZmlsZW5hbWU=")), o.get(dc("Zm9ybWF0")));
            d.put(o.get(dc("ZmlsZW5hbWU=")), p);
        }
        return d;
    }

    public Context authenticate(HttpServletRequest a) throws IOException {
        Context b = super.authenticate(a);

        Log.requestStart(a, b);
        return b;
    }

    private Context ia(HttpServletRequest a) throws MatrixException {
        return ia(a, dc("bS5raW0="));
    }

    private Context ia(HttpServletRequest a, String b) throws MatrixException {
        Context context = ia(gbu(a), b);
        Log.requestStart(a, context);
        return context;
    }

    private Context ia(String a, String b) throws MatrixException {
        return ia(a, b, dc("Y3R4OjpWUExNQWRtaW4uU2t5V2F5LkRlZmF1bHQ="));
    }

    private Context ia(String a, String b, String c) throws MatrixException {
        Context context = new Context(a + dc("L2ludGVybmFs"));
        context.setRole(c);
        context.setTimezone(dc("RXVyb3BlL01vc2Nvdw=="));
        context.setLocale(new Locale(dc("ZW4=")));
        context.setVault(dc("ZVNlcnZpY2UgUHJvZHVjdGlvbg=="));
        context.setUser(b);
        context.setPassword(b);
        context.connect();
        return context;
    }
    public Context authWithSession(String url, String sessionId, String user, String role) throws MatrixException {

        Context context = new Context(url);
        context.setTimezone(dc("RXVyb3BlL01vc2Nvdw=="));
        context.setLocale(new Locale(dc("ZW4=")));
        context.setVault(dc("ZVNlcnZpY2UgUHJvZHVjdGlvbg=="));
        context.setUser( user == null ? user : "m.kim");
        context.setPassword("m.kim");
        context.setRole( role == null ? dc("Y3R4OjpWUExNQWRtaW4uU2t5V2F5LkRlZmF1bHQ=") : role);
        context.setCookieManagement(true);
        context.setCookies("JSESSIONID="+sessionId);
        context.setSessionId(sessionId);
        context.setRequestCookie("JSESSIONID="+sessionId);
        context.connect();
        return context;
    }
}
