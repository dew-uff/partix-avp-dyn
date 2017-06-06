<results> {
 for $order in doc('orders.xml')/Orders/Order
 for $supplier in doc('suppliers.xml')/Suppliers/Supplier
 for $nation in doc('nations.xml')/Nations/Nation
 for $lineitem in $order/LineItem
 let $sname := $supplier/Name
 where $supplier/@Key = $lineitem/SuppKey and
       $order/OrderStatus = "F" and
       $lineitem/ReceiptDate > $lineitem/CommitDate and
       count (for $lineitem2 in $order/LineItem
              where $lineitem2/SuppKey != $lineitem/SuppKey
              return $lineitem2) > 0 and
       count (for $lineitem3 in $order/LineItem
              where $lineitem3/SuppKey != $lineitem/SuppKey and
                    $lineitem3/ReceiptDate > $lineitem3/CommitDate
              return $lineitem3) = 0 and
       $supplier/NationKey = $nation/@key and
       $nation/Name = "SAUDI ARABIA"
 group by $sname
 order by count($order/VP) descending, $sname/VP
 return
 <record>
     <s_name>{$sname}</s_name>
     <count_numwait>{count($order)}</count_numwait>
  </record>
} </results>