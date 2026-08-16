# Generates the legacy mipmap PNG launcher icons used on Android 7.x (API 24/25),
# where adaptive icons are unavailable. Keep the artwork in sync with
# app/src/main/res/drawable/ic_spotvault_mark.xml.
#
# Run from the repo root:
#   powershell -ExecutionPolicy Bypass -File generate_launcher_pngs.ps1

Add-Type -AssemblyName System.Drawing

function New-ArgbColor {
    param([int]$A, [int]$R, [int]$G, [int]$B)
    return [System.Drawing.Color]::FromArgb($A, $R, $G, $B)
}

function Add-RoundedRect {
    param($Path, [single]$X, [single]$Y, [single]$W, [single]$H, [single]$Radius)
    $d = $Radius * 2
    $Path.AddArc($X, $Y, $d, $d, 180, 90)
    $Path.AddArc($X + $W - $d, $Y, $d, $d, 270, 90)
    $Path.AddArc($X + $W - $d, $Y + $H - $d, $d, $d, 0, 90)
    $Path.AddArc($X, $Y + $H - $d, $d, $d, 90, 90)
    $Path.CloseFigure()
}

function Draw-SoftGlow {
    param($Graphics, [single]$CX, [single]$CY, [single]$Radius, $Color)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse($CX - $Radius, $CY - $Radius, $Radius * 2, $Radius * 2)
    $brush = New-Object System.Drawing.Drawing2D.PathGradientBrush($path)
    $brush.CenterColor = $Color
    $brush.SurroundColors = @([System.Drawing.Color]::FromArgb(0, $Color.R, $Color.G, $Color.B))
    $Graphics.FillPath($brush, $path)
    $brush.Dispose()
    $path.Dispose()
}

function Draw-BeaconIcon {
    param([int]$Size, [string]$Mask)

    $bmp = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $s = [single]$Size
    $shell = New-Object System.Drawing.Drawing2D.GraphicsPath
    if ($Mask -eq 'circle') {
        $shell.AddEllipse(0, 0, $s, $s)
    } elseif ($Mask -eq 'square') {
        $shell.AddRectangle((New-Object System.Drawing.RectangleF 0, 0, $s, $s))
    } else {
        Add-RoundedRect -Path $shell -X 0 -Y 0 -W $s -H $s -Radius ($s * 0.225)
    }

    $g.SetClip($shell)

    # Deep void background
    $bgRect = New-Object System.Drawing.RectangleF 0, 0, $s, $s
    $bgBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $bgRect,
        (New-ArgbColor 255 17 11 27),
        (New-ArgbColor 255 3 2 8),
        90
    )
    $g.FillRectangle($bgBrush, $bgRect)
    $bgBrush.Dispose()

    # Brand nebulae
    Draw-SoftGlow -Graphics $g -CX ($s * 0.26) -CY ($s * 0.22) -Radius ($s * 0.62) -Color (New-ArgbColor 90 91 33 182)
    Draw-SoftGlow -Graphics $g -CX ($s * 0.82) -CY ($s * 0.86) -Radius ($s * 0.55) -Color (New-ArgbColor 70 0 184 204)

    $cx = $s * 0.5
    $pinTop = $s * 0.185
    $pinBottom = $s * 0.83
    $halfW = $s * 0.25

    # Pin drop shadow
    $shadowBrush = New-Object System.Drawing.SolidBrush (New-ArgbColor 70 0 0 0)
    $g.FillEllipse($shadowBrush, ($cx - $s * 0.16), ($pinBottom - $s * 0.07), ($s * 0.32), ($s * 0.09))
    $shadowBrush.Dispose()

    # Pin teardrop
    $pin = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pin.AddBezier(
        $cx, $pinTop,
        ($cx - $halfW * 1.38), $pinTop,
        ($cx - $halfW * 1.02), ($pinTop + ($pinBottom - $pinTop) * 0.52),
        $cx, $pinBottom
    )
    $pin.AddBezier(
        $cx, $pinBottom,
        ($cx + $halfW * 1.02), ($pinTop + ($pinBottom - $pinTop) * 0.52),
        ($cx + $halfW * 1.38), $pinTop,
        $cx, $pinTop
    )
    $pin.CloseFigure()

    $pinRect = New-Object System.Drawing.RectangleF ($cx - $halfW * 1.4), $pinTop, ($halfW * 2.8), ($pinBottom - $pinTop)
    $pinBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $pinRect,
        (New-ArgbColor 255 226 220 254),
        (New-ArgbColor 255 59 7 100),
        60
    )
    $g.FillPath($pinBrush, $pin)
    $pinBrush.Dispose()

    # Rim light
    $rimPen = New-Object System.Drawing.Pen((New-ArgbColor 150 255 255 255), ($s * 0.012))
    $g.DrawPath($rimPen, $pin)
    $rimPen.Dispose()
    $pin.Dispose()

    # Beacon halo behind the dial
    $dialCY = $pinTop + ($pinBottom - $pinTop) * 0.39
    Draw-SoftGlow -Graphics $g -CX $cx -CY $dialCY -Radius ($s * 0.28) -Color (New-ArgbColor 120 0 240 255)

    # Vault outer ring
    $ringR = $s * 0.157
    $ringRect = New-Object System.Drawing.RectangleF ($cx - $ringR), ($dialCY - $ringR), ($ringR * 2), ($ringR * 2)
    $ringBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $ringRect,
        (New-ArgbColor 255 133 248 255),
        (New-ArgbColor 255 0 110 125),
        45
    )
    $g.FillEllipse($ringBrush, $ringRect)
    $ringBrush.Dispose()

    $ringPen = New-Object System.Drawing.Pen((New-ArgbColor 220 255 255 255), ($s * 0.008))
    $g.DrawEllipse($ringPen, $ringRect)
    $ringPen.Dispose()

    # Door recess
    $recessR = $s * 0.125
    $recessRect = New-Object System.Drawing.RectangleF ($cx - $recessR), ($dialCY - $recessR), ($recessR * 2), ($recessR * 2)
    $recessBrush = New-Object System.Drawing.SolidBrush (New-ArgbColor 255 15 9 25)
    $g.FillEllipse($recessBrush, $recessRect)
    $recessBrush.Dispose()

    # Six spokes and bolts
    $boltR = $s * 0.021
    $boltOrbit = $s * 0.148
    $spokeBrush = New-Object System.Drawing.SolidBrush (New-ArgbColor 255 0 240 255)
    $boltBrush = New-Object System.Drawing.SolidBrush (New-ArgbColor 255 210 252 255)
    for ($i = 0; $i -lt 6; $i++) {
        $angle = [Math]::PI * 2 * $i / 6 - [Math]::PI / 2
        $bx = $cx + $boltOrbit * [Math]::Cos($angle)
        $by = $dialCY + $boltOrbit * [Math]::Sin($angle)

        $ix = $cx + ($boltOrbit * 0.55) * [Math]::Cos($angle)
        $iy = $dialCY + ($boltOrbit * 0.55) * [Math]::Sin($angle)
        $spokePen = New-Object System.Drawing.Pen($spokeBrush.Color, ($s * 0.019))
        $g.DrawLine($spokePen, [single]$ix, [single]$iy, [single]$bx, [single]$by)
        $spokePen.Dispose()

        $g.FillEllipse($boltBrush, [single]($bx - $boltR), [single]($by - $boltR), [single]($boltR * 2), [single]($boltR * 2))
    }
    $spokeBrush.Dispose()
    $boltBrush.Dispose()

    # Lock hub gem
    $gemR = $s * 0.058
    $gemRect = New-Object System.Drawing.RectangleF ($cx - $gemR), ($dialCY - $gemR), ($gemR * 2), ($gemR * 2)
    $gemBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $gemRect,
        (New-ArgbColor 255 245 243 255),
        (New-ArgbColor 255 91 33 182),
        45
    )
    $g.FillEllipse($gemBrush, $gemRect)
    $gemBrush.Dispose()

    $gemPen = New-Object System.Drawing.Pen((New-ArgbColor 120 255 255 255), ($s * 0.007))
    $g.DrawEllipse($gemPen, $gemRect)
    $gemPen.Dispose()

    # Keyhole
    $keyBrush = New-Object System.Drawing.SolidBrush (New-ArgbColor 255 10 6 18)
    $keyR = $s * 0.019
    $g.FillEllipse($keyBrush, ($cx - $keyR), ($dialCY - $keyR * 1.4), ($keyR * 2), ($keyR * 2))
    $g.FillRectangle($keyBrush, ($cx - $keyR * 0.55), ($dialCY - $keyR * 0.1), ($keyR * 1.1), ($s * 0.038))
    $keyBrush.Dispose()

    $g.ResetClip()
    $g.Dispose()
    $shell.Dispose()
    return $bmp
}

$densities = [ordered]@{
    mdpi    = 48
    hdpi    = 72
    xhdpi   = 96
    xxhdpi  = 144
    xxxhdpi = 192
}

foreach ($entry in $densities.GetEnumerator()) {
    $folder = Join-Path "app\src\main\res" ("mipmap-" + $entry.Key)
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    $size = $entry.Value

    $square = Draw-BeaconIcon -Size $size -Mask 'rounded'
    $square.Save((Join-Path $folder "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $square.Dispose()

    $round = Draw-BeaconIcon -Size $size -Mask 'circle'
    $round.Save((Join-Path $folder "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $round.Dispose()

    Write-Host "Generated $($entry.Key) ($size px)"
}

# Play Store listing asset (512x512, square, no transparency requirement).
$storeDir = "store-assets"
New-Item -ItemType Directory -Force -Path $storeDir | Out-Null
$store = Draw-BeaconIcon -Size 512 -Mask 'square'
$store.Save((Join-Path $storeDir "play_store_icon_512.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$store.Dispose()
Write-Host "Generated Play Store icon (512 px) -> $storeDir\play_store_icon_512.png"

Write-Host "Done."
