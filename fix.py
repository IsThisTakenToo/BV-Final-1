with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'r') as f:
    lines = f.readlines()

for i in range(1195, 1205):
    if "            // New centered detail view layout" in lines[i]:
        # insert '} else {' before it
        lines.insert(i, "        } else {\n")
        
        # also we need to check if there is an extra '}'
        # the lines before are:
        #                 } // end button Box
        #             } // end main Box
        #             }
        break

with open('app/src/main/java/com/spotvault/app/MainActivity.kt', 'w') as f:
    f.writelines(lines)
