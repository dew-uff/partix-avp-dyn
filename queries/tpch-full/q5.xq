<results> {
 for $part in doc('parts.xml')/Parts/Part
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 let $ptype := $part/Type     
 let $region := $nation/RegionKey
 let $nname := $nation/Name
 let $pavail := $part/PartSupp/Avail
 where $supplier/NationKey = $nation/@key and
       $part/PartSupp/Suppkey = $supplier/@Key
 group by $ptype, $region, $nname
 order by $ptype/VP, avg($pavail/VP) descending
 return
 <record>
     <p_type>{$ptype}</p_type>
     <p_avail>{$pavail}</p_avail>
     <avg_avail>{avg($pavail)}</avg_avail>
     <n_name>{$nname}</n_name>
     <r_region>{$region}</r_region>
  </record>
} </results>