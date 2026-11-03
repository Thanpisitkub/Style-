# Style — สไตล์หมัด Epic Fight 10 สไตล์ (สเปคดีไซน์)

วันที่: 2026-11-03 · สถานะ: **รอผู้ใช้ตรวจ** · ยังไม่มีโค้ด

## 1. เป้าหมาย
ม็อด Forge 1.20.1 ชื่อ "Style" ที่เพิ่มไอเท็มสไตล์หมัด 10 ชิ้นให้ Epic Fight ใช้อนิเมชั่นจากม็อดที่ผู้ใช้ส่งมา เสกได้จาก Creative ไอคอนตัว S ขาวบนพื้นดำ ถือแล้วไม่เห็นไอเท็มในมือ

## 2. ความต้องการ (จากผู้ใช้)
- 10 สไตล์ ชื่อ "Style 1" ถึง "Style 10" (ผู้ใช้ตอบว่าชื่อคือ "Style")
- ทุกสไตล์: ต่อย 1–6, วิ่งแล้วต่อย 1 ท่า, กระโดดแล้วต่อย 1 ท่า
- ต่อย 4 = ท่าซัดศัตรูขึ้นฟ้า ต่อย 5–6 = คอมโบต่อจากท่า 4
- ท่ายืนของทุกสไตล์ใช้อนิเมชั่นของ WOM
- ดาเมจ 4.00–8.00 ความเร็ว 1.50–2.50 มีทั้งชุดแรงช้าและชุดเร็วเบา
- ปนท่าเตะได้ (ผู้ใช้ยืนยันแล้ว)
- ส่งมอบเป็น .jar ผ่าน GitHub Actions ของรีโปนี้ (ผู้ใช้เล่นบนมือถือ)

## 3. ผลสำรวจม็อดที่ส่งมา
| ม็อด | สถานะ | ใช้ในม็อดนี้ |
|---|---|---|
| Weapons of Miracles 2.0.171 (WOM) | Epic Fight API รุ่นใหม่ (`AnimationAccessor`) ต้องการ Epic Fight ≥ 20.14.3 | ใช้: ท่าต่อย/เตะ และท่ายืน/เดิน/วิ่ง |
| ef_cz 0.3.9.10 | API รุ่นใหม่ | ใช้: ท่าของ kabal, ironclad, shogun |
| effs 1.1.0 | มีสไตล์ "Hand Combat" แบบ JSON | ใช้เป็นแม่แบบโครงสร้างเท่านั้น |
| EpicSanji 20.1.6.1 | build ก.พ. 2024 ใช้ API รุ่นเก่า (`api.forgeevent.AnimationRegistryEvent`) | **ไม่ใช้** ดูข้อ 9 |

## 4. แนวทาง
ข้อมูลเป็น JSON แบบ data-driven ของ Epic Fight (โครงสร้างเดียวกับ `effs:hand_combat`) มี Java เฉพาะส่วนที่ JSON ทำไม่ได้ คือลงทะเบียนไอเท็ม แท็บ Creative ค่าดาเมจ/ความเร็ว และ hook ซัดศัตรูขึ้นฟ้า ไม่ลงทะเบียนอนิเมชั่นใหม่ จึงไม่ต้องคอมไพล์ผูกกับโค้ด Epic Fight

## 5. โครงสร้างรีโป
```
build.gradle, settings.gradle, gradle.properties     ForgeGradle 6, Forge 1.20.1 (47.x), Java 17
.github/workflows/build.yml                          build + แนบ .jar เป็น Release `latest`
tools/styles.json                                    ตารางเดียวของ 10 สไตล์ (ข้อ 7)
tools/generate.py                                    สร้าง JSON ทั้งหมดจากตาราง + ตรวจชื่อท่ากับ jar
src/main/java/.../FistStyleMod.java                  ลงทะเบียนไอเท็ม + แท็บ Creative
src/main/java/.../StyleItem.java                     ไอเท็ม พร้อมค่าดาเมจ/ความเร็ว
src/main/java/.../LaunchHandler.java                 hook ท่าที่ 4 (ข้อ 8)
src/main/resources/META-INF/mods.toml, pack.mcmeta
src/main/resources/assets/fiststyle/...              lang, models/item, textures/item/style_s.png
src/main/resources/data/fiststyle/capabilities/weapons/style_N.json และ types/style_N.json
```
mod id: `fiststyle` ไอเท็ม `fiststyle:style_1` … `style_10`

## 6. ไอเท็ม
- ไอเท็มเดียวชนิดเดียวกัน 10 ชิ้น `stacksTo(1)` ชื่อ "Style N" แท็บ Creative ชื่อ "Style" ไอคอนแท็บคือ Style 1
- texture 16×16 ตัว S ขาวบนพื้นดำ สร้างด้วยสคริปต์ ใช้ร่วมกันทั้ง 10 ชิ้น
- model `item/handheld` ตั้ง `display` ของมือขวา/ซ้ายทั้งมุมมองบุคคลที่หนึ่งและสามเป็น scale 0 (วิธีเดียวกับ effs)
- ดาเมจ = ค่าในตารางข้อ 7 ตั้งเป็น attribute modifier ของมือหลัก (ค่าที่ตั้ง = ดาเมจ − 1.0 เพราะผู้เล่นมีฐาน 1.0) ความเร็ว = ค่าในตาราง (ตั้ง = ความเร็ว − 4.0) ดาเมจต่อหมัดจริงจะคูณด้วยตัวคูณของแต่ละอนิเมชั่นอีกที

## 7. ตารางสไตล์
| สไตล์ | ดาเมจ | ความเร็ว | impact |
|---|---|---|---|
| Style 1 | 4.00 | 2.50 | 0.50 |
| Style 2 | 4.50 | 2.40 | 0.65 |
| Style 3 | 5.00 | 2.25 | 0.80 |
| Style 4 | 5.50 | 2.10 | 0.95 |
| Style 5 | 6.00 | 2.00 | 1.10 |
| Style 6 | 6.25 | 1.90 | 1.25 |
| Style 7 | 6.50 | 1.80 | 1.40 |
| Style 8 | 7.00 | 1.70 | 1.55 |
| Style 9 | 7.50 | 1.60 | 1.70 |
| Style 10 | 8.00 | 1.50 | 1.85 |

`max_strikes` = 1 ทุกสไตล์ `armor_negation` = 0 ค่าเหล่านี้ผู้ใช้ไม่ได้ระบุ ตั้งไว้เป็นค่าเริ่มต้นเพื่อให้แก้ง่าย

ท่าแต่ละสไตล์ (W = WOM, C = ef_cz, ตรวจแล้วว่าชื่อทุกท่ามีลงทะเบียนจริงในม็อดนั้น) ท่า 7–8 เป็นช่อง "วิ่งแล้วต่อย" และ "กระโดดแล้วต่อย" ตามที่ Epic Fight กำหนดให้สองท่าท้ายสุดของรายการคอมโบ

### Style 1 — ดาเมจ 4.00 / ความเร็ว 2.50 — ท่ายืน WOM `enderblaster_onehand`
1. ต่อย 1: `W:enderblaster_onehand_auto_1`
2. ต่อย 2: `C:kabal_auto1`
3. ต่อย 3: `W:enderblaster_onehand_auto_2`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `C:kabal_auto2`
5. ต่อย 5: `W:enderblaster_onehand_auto_3`
6. ต่อย 6: `W:enderblaster_onehand_auto_4`
7. วิ่งแล้วต่อย: `C:kabal_dash1`
8. กระโดดแล้วต่อย: `W:enderblaster_onehand_jumpkick`

### Style 2 — ดาเมจ 4.50 / ความเร็ว 2.40 — ท่ายืน WOM `enderblaster_twohand`
1. ต่อย 1: `C:kabal_auto1`
2. ต่อย 2: `W:enderblaster_twohand_auto_1`
3. ต่อย 3: `W:enderblaster_twohand_auto_2`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:enderblaster_twohand_tishnaw`
5. ต่อย 5: `W:enderblaster_twohand_auto_3`
6. ต่อย 6: `W:enderblaster_twohand_auto_4`
7. วิ่งแล้วต่อย: `W:enderblaster_twohand_dash`
8. กระโดดแล้วต่อย: `C:kabal_airslash`

### Style 3 — ดาเมจ 5.00 / ความเร็ว 2.25 — ท่ายืน WOM `nova`
1. ต่อย 1: `W:enderblaster_twohand_auto_1`
2. ต่อย 2: `C:kabal_auto1`
3. ต่อย 3: `W:enderblaster_onehand_auto_3`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:kick_1`
5. ต่อย 5: `C:kabal_auto3`
6. ต่อย 6: `W:kick`
7. วิ่งแล้วต่อย: `C:kabal_dash2`
8. กระโดดแล้วต่อย: `W:enderblaster_onehand_jumpkick`

### Style 4 — ดาเมจ 5.50 / ความเร็ว 2.10 — ท่ายืน WOM `agony`
1. ต่อย 1: `C:ironclad_auto1`
2. ต่อย 2: `C:ironclad_auto2`
3. ต่อย 3: `W:enderblaster_twohand_auto_2`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:agony_clawstrike`
5. ต่อย 5: `W:enderblaster_twohand_auto_4`
6. ต่อย 6: `C:kabal_auto3`
7. วิ่งแล้วต่อย: `C:ironclad_dash`
8. กระโดดแล้วต่อย: `W:agony_airslash`

### Style 5 — ดาเมจ 6.00 / ความเร็ว 2.00 — ท่ายืน WOM `moonless`
1. ต่อย 1: `C:ironclad_auto1`
2. ต่อย 2: `C:shogun_auto1`
3. ต่อย 3: `C:ironclad_auto2`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `C:ironclad_fist_charge`
5. ต่อย 5: `C:shogun_auto2`
6. ต่อย 6: `W:kick_2`
7. วิ่งแล้วต่อย: `C:shogun_dash1`
8. กระโดดแล้วต่อย: `C:ironclad_airslash`

### Style 6 — ดาเมจ 6.25 / ความเร็ว 1.90 — ท่ายืน WOM `napoleon`
1. ต่อย 1: `C:shogun_auto1`
2. ต่อย 2: `C:shogun_auto2`
3. ต่อย 3: `W:antitheus_ascended_auto_1`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:kick`
5. ต่อย 5: `W:antitheus_ascended_auto_2`
6. ต่อย 6: `W:antitheus_ascended_auto_3`
7. วิ่งแล้วต่อย: `C:shogun_dash2`
8. กระโดดแล้วต่อย: `W:agony_air_attack_1`

### Style 7 — ดาเมจ 6.50 / ความเร็ว 1.80 — ท่ายืน WOM `orbit`
1. ต่อย 1: `W:antitheus_ascended_auto_1`
2. ต่อย 2: `W:antitheus_auto_1`
3. ต่อย 3: `W:antitheus_ascended_auto_2`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:strong_punch`
5. ต่อย 5: `W:antitheus_auto_3`
6. ต่อย 6: `W:antitheus_ascended_auto_3`
7. วิ่งแล้วต่อย: `W:torment_berserk_dash`
8. กระโดดแล้วต่อย: `W:agony_air_attack_2`

### Style 8 — ดาเมจ 7.00 / ความเร็ว 1.70 — ท่ายืน WOM `antitheus`
1. ต่อย 1: `W:antitheus_auto_1`
2. ต่อย 2: `W:antitheus_auto_2`
3. ต่อย 3: `W:antitheus_auto_3`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:antitheus_guillotine`
5. ต่อย 5: `W:antitheus_auto_4`
6. ต่อย 6: `W:torment_berserk_auto_1`
7. วิ่งแล้วต่อย: `W:torment_dash`
8. กระโดดแล้วต่อย: `W:agony_air_attack_3`

### Style 9 — ดาเมจ 7.50 / ความเร็ว 1.60 — ท่ายืน WOM `torment_berserk`
1. ต่อย 1: `W:antitheus_auto_2`
2. ต่อย 2: `W:torment_berserk_auto_1`
3. ต่อย 3: `W:antitheus_auto_3`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:antitheus_agression`
5. ต่อย 5: `W:kick_2`
6. ต่อย 6: `W:torment_berserk_auto_2`
7. วิ่งแล้วต่อย: `W:torment_berserk_dash`
8. กระโดดแล้วต่อย: `W:torment_berserk_airslam`

### Style 10 — ดาเมจ 8.00 / ความเร็ว 1.50 — ท่ายืน WOM `antitheus_ascended`
1. ต่อย 1: `W:torment_berserk_auto_2`
2. ต่อย 2: `W:kick_1`
3. ต่อย 3: `W:antitheus_auto_4`
4. ต่อย 4 ⬆ ซัดขึ้นฟ้า: `W:torment_charged_attack_2`
5. ต่อย 5: `W:kick_3`
6. ต่อย 6: `W:antitheus_ascended_auto_3`
7. วิ่งแล้วต่อย: `W:torment_dash`
8. กระโดดแล้วต่อย: `W:torment_airslam`

ท่ายืนใช้ `wom:biped/living/<ชื่อ>_idle` ท่าเดินและวิ่งใช้ `<ชื่อ>_walk` / `<ชื่อ>_run` ของชุดเดียวกัน (Style 2 ไม่มี `run` จะใช้ค่าเริ่มต้นของเกม)

ท่าชุดนี้เลือกจากชื่อ ประเภทอนิเมชั่น ความยาว และ stun type ที่อ่านจากโค้ดของม็อด ฉันดูภาพอนิเมชั่นหรือเล่นเกมทดสอบไม่ได้ ท่าไหนดูแปลกให้แจ้งเลขสไตล์กับเลขท่า แก้เป็นแถวเดียวใน `tools/styles.json` แล้ว build ใหม่

## 8. ท่าที่ 4 ซัดศัตรูขึ้นฟ้า
ไม่พบท่าที่ระบุว่าซัดศัตรูขึ้นฟ้า (ตรวจจากชื่อท่าและโค้ดของม็อด) บางท่ามี stun type แบบ FALL/KNOCKDOWN ซึ่งล้มเป้าหมายแต่ไม่ใช่การซัดขึ้น จึงเพิ่ม hook ใน Java:
- ฟัง `LivingHurtEvent` ฝั่งเซิร์ฟเวอร์ เมื่อผู้โจมตีเป็นผู้เล่นที่ถือไอเท็มสไตล์ และแหล่งดาเมจมาจากอนิเมชั่น "ต่อย 4" ของสไตล์นั้น ตั้งความเร็วแนวดิ่งให้เป้าหมายพุ่งขึ้น และให้ slow falling สั้น ๆ เพื่อให้ท่า 5–6 ต่อติด ค่าความสูงและเวลาเป็นค่าคงที่ที่ปรับได้
- ใช้ reflection อ่านชื่ออนิเมชั่นจากแหล่งดาเมจของ Epic Fight จึงคอมไพล์ได้โดยไม่ต้องมี Epic Fight jar ถ้าอ่านไม่ได้ (API ไม่ตรงรุ่น) hook จะบันทึก warning หนึ่งครั้งแล้วไม่ทำอะไร ม็อดยังใช้งานได้แต่หมัด 4 จะไม่ซัดขึ้น
- ฉันทดสอบ hook นี้เองไม่ได้ ต้องให้ผู้ใช้ลองในเกม

## 9. ข้อจำกัดและความเสี่ยง
1. **EpicSanji ใช้ไม่ได้ร่วมกับ WOM/ef_cz ในรุ่นที่ส่งมา** build เมื่อ ก.พ. 2024 ลงทะเบียนอนิเมชั่นด้วย API ที่ WOM/ef_cz (ซึ่งต้องการ Epic Fight ≥ 20.14.3) ไม่ใช้แล้ว ดังนั้นสไตล์ทั้งหมดจึงไม่ใช้ท่าของ Sanji ท่าเตะมาจาก WOM แทน (`kick`, `kick_1–3`, `jumpkick`) ถ้าผู้ใช้รัน EpicSanji ได้จริงในแพ็กเดียวกัน แจ้งได้ ฉันจะเพิ่มชุดท่า Sanji
2. **ฉันคอมไพล์และรันเกมที่นี่ไม่ได้** (เซิร์ฟเวอร์เข้า Forge maven ไม่ได้) การยืนยันทำได้เท่านี้: GitHub Actions build ผ่าน, ไฟล์ JSON ถูกต้อง, ชื่อท่าทุกท่าตรงกับ jar ที่ส่งมา ส่วนการทำงานในเกมผู้ใช้ต้องทดสอบ
3. สคีมา JSON อ้างอิง `effs` ซึ่งเป็นตัวอย่างที่ใช้ได้จริงกับ Epic Fight รุ่นเดียวกับ WOM ถ้าเกมแจ้ง error ตอนโหลดข้อมูล ให้ส่งบรรทัด error มา
4. ม็อดนี้ประกาศพึ่งพา `epicfight` (≥ 20.14.3), `wom`, `ef_cz` ถ้าขาดตัวใดตัวหนึ่ง เกมจะแจ้งตอนเปิด

## 10. ส่งมอบ
- โค้ดอยู่บน branch `claude/fist-styles` (ไม่แตะ `main` จนกว่าผู้ใช้สั่ง)
- GitHub Actions build ทุก push แล้วอัปโหลด `.jar` เป็น Release ชื่อ `latest` ผู้ใช้โหลดจากหน้า Releases บนมือถือได้โดยตรง
- ก่อนใช้ ต้องมี Epic Fight, WOM และ ef_cz อยู่ในแพ็ก
