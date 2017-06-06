<results> {
let $auction := doc("xmark100m.xml") return
 <total_prices_higher_forty> { count( for $i in $auction/site/closed_auctions/closed_auction
  where $i/price/text() >= 40 return   $i/price ) } </total_prices_higher_forty>
} </results>