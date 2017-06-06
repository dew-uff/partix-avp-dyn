<results> {
let $auction := doc("xmark100m.xml") return
 for $a in $auction/site/closed_auctions/closed_auction
 where
  not(
    empty(
      $a/annotation/description/parlist/listitem/parlist/listitem/text/emph/
       keyword/
       text()
    )
  )
 return <sellers_with_emph_keywords> { <person id="{$a/seller/@person}"/> } </sellers_with_emph_keywords>
} </results>