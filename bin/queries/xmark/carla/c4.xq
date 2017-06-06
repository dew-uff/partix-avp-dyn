<results>
 {
 for $inp in doc('dblp.xml')/dblp/inproceedings
  where count($inp/author) = 1
  return
 <inproceeding_one_author>
 { $inp }
 </inproceeding_one_author>
 }
 </results>