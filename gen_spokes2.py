import math

cx = 54
cy = 42
r_inner = 9.0
r_outer = 17.0
half_width = 1.5

path = []

for i in range(8):
    angle = math.radians(i * 45 - 90)
    
    dir_x = math.cos(angle)
    dir_y = math.sin(angle)
    
    perp_x = -math.sin(angle)
    perp_y = math.cos(angle)
    
    p1x = cx + r_inner * dir_x + half_width * perp_x
    p1y = cy + r_inner * dir_y + half_width * perp_y
    
    p2x = cx + r_inner * dir_x - half_width * perp_x
    p2y = cy + r_inner * dir_y - half_width * perp_y
    
    p3x = cx + r_outer * dir_x - half_width * perp_x
    p3y = cy + r_outer * dir_y - half_width * perp_y
    
    p4x = cx + r_outer * dir_x + half_width * perp_x
    p4y = cy + r_outer * dir_y + half_width * perp_y
    
    path.append(f"M{p1x:.2f},{p1y:.2f} L{p2x:.2f},{p2y:.2f} L{p3x:.2f},{p3y:.2f} L{p4x:.2f},{p4y:.2f} Z")

print(" ".join(path))
