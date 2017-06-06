<results>
{
 for $op in doc('xmark.xml')/site/open_auctions/open_auction
 let $bd := $op/bidder
 where count($op/bidder) > 5
 return
 <record>
 <open_auctions_with_more_than_5_bidders>
 <auction> {$op} </auction>
 <qty_bidder> {count($op/bidder)} </qty_bidder>
 </open_auctions_with_more_than_5_bidders>
 </record>
 } </results>