sed -i '/NavigationBarItem(/,/)/!b;//!d;/NavigationBarItem(/,/)/{
  /NavigationBarItem(/h
  /NavigationBarItem(/!H
  /)/{
    g
    p
  }
  d
}' ./app/src/main/java/com/spotvault/app/SpotVaultDesign.kt
