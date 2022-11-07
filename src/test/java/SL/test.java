package SL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class test {
    public static List<List<String>> combinations(List<String> c, int ke){
        if(ke>c.size()){
            return new ArrayList<List<String>>();
        }
        List<List<String>> ans = new ArrayList<List<String>>();
        if(ke==1){
            for(String ci:c){
                List<String> ttt = new ArrayList<String>();
                ttt.add(ci);
                ans.add(ttt);
            }
            return ans;
        }
        String first = c.get(0);
        List<String> c_copy = new ArrayList<String>();
        c_copy.addAll(c);
        c_copy.remove(0);
        List<List<String>> tmp = combinations(c_copy,ke-1);
        for(List<String> t:tmp){
            t.add(0, first);
        }
        ans.addAll(tmp);
        List<List<String>> tmp2 = combinations(c_copy, ke);
        ans.addAll(tmp2);
        return ans;
    }

    public static void main(String[] args) {
        List<String> ll = new ArrayList<String>();
        ll.add("A");
        ll.add("K");
        ll.add("D");
        ll.add("F");
        ll.add("W");
        ll.add("B");
        List<List<String>> ans = combinations(ll, 6);
//        for (List<String> a:ans){
//            System.out.print(a);
//        }
        System.out.println(ans.size());
    }
}


