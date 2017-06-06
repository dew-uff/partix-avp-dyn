<results> {
 for $lineitem in doc('orders.xml')/Orders/Order/LineItem
 let $returnflag := $lineitem/ReturnFlag
 let $linestatus := $lineitem/LineStatus
 let $extendedprice := $lineitem/ExtendedPrice
 let $discountprice := $lineitem/DiscountPrice
 let $charge := $lineitem/Charge
 let $quantity := $lineitem/Quantity
 where $lineitem/ShipDate <= "1998-08-31"
 group by $returnflag, $linestatus
 order by $returnflag/VP, $linestatus/VP
 return
 <record>
    <l_returnflag>{$returnflag}</l_returnflag>
    <l_linestatus>{$linestatus}</l_linestatus>
    <sum_qty>{sum($quantity)}</sum_qty>
    <sum_base_price>{sum($extendedprice)}</sum_base_price>
    <sum_disc_price>{sum($discountprice)}</sum_disc_price>
    <sum_charge>{sum($charge)}</sum_charge>
    <avg_qty>{avg($quantity)}</avg_qty>
    <avg_price>{avg($extendedprice)}</avg_price>
    <avg_disc>{avg($discountprice)}</avg_disc>
    <count_order>{count($lineitem)}</count_order>
 </record>
} </results>