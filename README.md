# Style — สไตล์หมัด Epic Fight

ม็อด Forge 1.20.1 ที่เพิ่มไอเท็ม **Style 1** (สไตล์เดียว) ให้ Epic Fight ใช้ท่าจาก Weapons of Miracles และ EpicSanji มีระบบ Auto Parry / Auto Dodge

- เสกได้จากแท็บ Creative ชื่อ **Style** ไอคอนตัว S ขาวบนพื้นดำ ถือแล้วมองไม่เห็นไอเท็มในมือ
- ดาเมจ 4.00 ความเร็วโจมตี 2.10 impact 1.1 ทะลุเกราะ 50
- คอมโบ 7 ท่า: `solar_auto_1_polvora`, `sanji_auto_1`, `sanji_diable_auto_2`, `sanji_auto_3` (ท่าที่ 4 ปล่อยศัตรูลอย), `sanji_diable_auto_1`, ท่าวิ่ง `enderblaster_twohand_tishnaw`, ท่ากระโดด `sanji_antimaner`
- ท่ายืน/เดิน/วิ่งเป็นของ WOM solar
- ใช้สกิลทั้งหมดของ Epic Fight ได้ ไม่ใช้ท่าอาวุธปกติของ Epic Fight
- ไม่ใช้ท่าที่ทำให้เซิร์ฟเวอร์แครชเมื่อผู้เล่นไม่มีสกิลนั้น (`tools/unsafe_animations.json`)

## Auto Parry / Auto Dodge
| ปุ่ม | ทำอะไร |
|---|---|
| `V` | เปิด/ปิด Auto Parry |
| `B` | เปิด/ปิด Auto Dodge |
| `G` | เปิดหน้าตั้งค่า |

เปลี่ยนปุ่มได้ที่ Options → Controls → Style ติดตั้งทั้งฝั่ง client และ server

## แก้ไขชุดท่า
แก้ `tools/styles.json` แล้วรัน `python3 tools/generate.py` (ไฟล์ใน `src/main/resources` สร้างจากสคริปต์นี้)
