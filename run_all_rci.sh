sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih.log /home/fiedlda1/Amodsim/rci_lounchers/ih.sh
sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-sw.log /home/fiedlda1/Amodsim/rci_lounchers/ih-sw.sh
sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr.log /home/fiedlda1/Amodsim/rci_lounchers/ih-nr.sh
sbatch --mem=25 -t 60 -n30 -o /home/fiedlda1/Amodsim/log/ih-nr-sw.log /home/fiedlda1/Amodsim/rci_lounchers/ih-nr-sh.sh
sbatch --mem=75 -t 1500 -n30 -p longjobs -o /home/fiedlda1/Amodsim/log/vga.log /home/fiedlda1/Amodsim/rci_lounchers/vga.sh
sbatch --mem=75 -t 1500 -n30 -p longjobs -o /home/fiedlda1/Amodsim/log/vga-sw.log /home/fiedlda1/Amodsim/rci_lounchers/vga-sw.sh
sbatch --mem=75 -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim.log /home/fiedlda1/Amodsim/rci_lounchers/vga-lim.sh
sbatch --mem=75 -t 600 -n30 -o /home/fiedlda1/Amodsim/log/vga-lim-sw.log /home/fiedlda1/Amodsim/rci_lounchers/vga-lim-sw.sh