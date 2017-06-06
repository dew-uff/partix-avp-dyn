<results> {
 for $part in doc('parts.xml')/Parts/Part
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 let $ptype := $part/Type
 let $region := $nation/RegionKey
 where $part/PartSupp/Suppkey = $supplier/@Key and
       $supplier/NationKey = $nation/@key 
 group by $ptype, $region
 order by $ptype/VP, avg($part/PartSupp/Avail/VP) descending 
 return
 <record>
     <p_type>{$ptype}</p_type>
     <avg_avail>{avg($part/PartSupp/Avail)}</avg_avail>
     <n_nations>{$nation/Name}</n_nations>
  </record>
} </results>